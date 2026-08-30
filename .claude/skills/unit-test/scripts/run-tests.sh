#!/usr/bin/env bash
#
# unit-test skill helper
# 定位被测类所属模块，在模块内执行其 SpringBootTest，并聚合 surefire 报告输出。
#
# 用法:
#   run-tests.sh <被测类>
#     <被测类> 支持三种写法:
#       1) 源码文件路径(相对仓库根):  src/main/java/com/yjw/service/OrderService.java
#       2) 全限定类名:               com.yjw.service.OrderService
#       3) 简单类名:                 OrderService
#
# 依赖: bash3.2+、mvn 在 PATH、当前在 git 仓库内。

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
cd "$ROOT" || { echo "无法进入仓库根目录: $ROOT"; exit 2; }

TARGET="${1:-}"
if [ -z "$TARGET" ]; then
  echo "用法: run-tests.sh <被测类 文件路径|FQCN|类名>"
  exit 2
fi

# ---------- 解析被测类路径 ----------
SRC=""
if [ -f "$TARGET" ]; then
  SRC="$TARGET"
elif [ -f "$ROOT/$TARGET" ]; then
  SRC="$ROOT/$TARGET"
else
  BASE="$(basename "$TARGET")"
  BASE="${BASE##*.}"   # FQCN 如 com.yjw.GatewayApplication -> GatewayApplication
  SRC="$(grep -rl --include='*.java' "class $BASE[[:space:]]" "$ROOT" 2>/dev/null | grep -v '/test/' | head -n1 || true)"
fi

if [ -z "$SRC" ] || [ ! -f "$SRC" ]; then
  echo "未找到被测类: $TARGET"
  exit 2
fi
SRC="$(cd "$(dirname "$SRC")" && pwd)/$(basename "$SRC")"

if echo "$SRC" | grep -q '/test/'; then
  echo "目标是测试类（路径含 /test/），请传入 src/main 下的被测类。"
  exit 2
fi

PKG="$(grep -m1 '^package ' "$SRC" | sed 's/package //; s/;//' | tr -d '[:space:]')"
BASENAME="$(basename "$SRC" .java)"
TEST_CLASS="${BASENAME}Test"

# ---------- 定位所属 Maven 模块 ----------
MODULE_DIR="$SRC"
while [ "$MODULE_DIR" != "$ROOT" ]; do
  if [ -f "$MODULE_DIR/pom.xml" ]; then
    break
  fi
  MODULE_DIR="$(dirname "$MODULE_DIR")"
done
MODULE_REL="${MODULE_DIR#"$ROOT"/}"

TEST_FILE="$MODULE_DIR/src/test/java/${PKG//.//}/$TEST_CLASS.java"

echo "被测类   : $PKG.$BASENAME"
echo "测试类   : $PKG.$TEST_CLASS"
echo "模块     : $MODULE_REL"
echo "测试文件 : $TEST_FILE"

# 依赖检查：模块 pom 若无，则向上查父 pom（子模块可能继承父级 test 依赖）
HAS_TEST_DEPS=""
DIR="$MODULE_DIR"
while [ "$DIR" != "/" ]; do
  if [ -f "$DIR/pom.xml" ] && grep -q 'spring-boot-starter-test' "$DIR/pom.xml"; then
    HAS_TEST_DEPS="yes"
    break
  fi
  [ "$DIR" = "$ROOT" ] && break
  DIR="$(dirname "$DIR")"
done
if [ -z "$HAS_TEST_DEPS" ]; then
  echo "⚠ 模块 $MODULE_REL 及其父 pom 均未声明 spring-boot-starter-test(test scope)，请先补充依赖再运行。"
fi
if [ ! -f "$TEST_FILE" ]; then
  echo "⚠ 未找到测试类文件: $TEST_FILE"
  echo "  请先通过 /unit-test 技能生成测试后再运行本脚本。"
  exit 2
fi

# ---------- 执行 mvn test ----------
# 在模块目录内执行，避免读取仓库根聚合 reactor（本仓库根 reactor 存在损坏 pom 模块，
# 从根目录 -pl/-am 构建会因无关模块的父 POM 解析失败而整体报错）。
LOG="$(mktemp /tmp/unit-test-mvn.XXXXXX)"
echo "==> 执行测试: $TEST_CLASS (模块: $MODULE_REL)"
echo "    (cd $MODULE_REL && mvn test -Dtest=$TEST_CLASS -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false)"
(cd "$MODULE_DIR" && mvn test -Dtest="$TEST_CLASS" \
  -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false) \
  >"$LOG" 2>&1
MVN_STATUS=$?

BUILD_LINE="$(grep -E 'BUILD (SUCCESS|FAILURE)' "$LOG" | tail -1 || true)"
if [ -z "$BUILD_LINE" ]; then
  # mvn 可能在编译阶段就失败，或不是标准输出
  BUILD_LINE="BUILD UNKNOWN (mvn 退出码 $MVN_STATUS)"
fi

# ---------- 聚合 surefire 报告 ----------
REPORTS_DIR="$MODULE_DIR/target/surefire-reports"
RESULT=""
if [ -d "$REPORTS_DIR" ] && ls "$REPORTS_DIR"/*.txt >/dev/null 2>&1; then
  RESULT="$(awk '/Tests run:/ {
      for (i = 1; i <= NF; i++) {
        gsub(",", "", $i)
        if ($i == "run:")      run += $(i + 1)
        else if ($i == "Failures:") f += $(i + 1)
        else if ($i == "Errors:")   e += $(i + 1)
        else if ($i == "Skipped:")  s += $(i + 1)
      }
    }
    END { print run, f, e, s }' "$REPORTS_DIR"/*.txt 2>/dev/null)"
fi

FAILED_METHODS=""
if [ -d "$REPORTS_DIR" ]; then
  FAILED_METHODS="$(awk '/<<< (FAILURE|ERROR)/ { sub(/[[:space:]]*<<<.*/, ""); print }' "$REPORTS_DIR"/*.txt 2>/dev/null | sort -u)"
fi

# ---------- 输出报告 ----------
echo
echo "================ 测试报告 ================"
echo "测试类   : $PKG.$TEST_CLASS"
echo "模块     : $MODULE_REL"
echo "------------------------------------------"
if [ -n "$RESULT" ]; then
  read -r R_TOTAL R_FAILS R_ERRORS R_SKIPS <<<"$RESULT"
  echo "Tests run: ${R_TOTAL:-0} | Failures: ${R_FAILS:-0} | Errors: ${R_ERRORS:-0} | Skipped: ${R_SKIPS:-0}"
else
  echo "未聚合到 surefire 用例结果（可能编译失败或未执行测试）。"
fi
echo "------------------------------------------"
if [ -n "$FAILED_METHODS" ]; then
  echo "失败/错误用例:"
  echo "$FAILED_METHODS"
  echo "（完整堆栈见 $REPORTS_DIR 下对应 .txt / .xml）"
else
  echo "无失败用例。"
fi
echo "------------------------------------------"
echo "构建结果: $BUILD_LINE"
[ -f "$LOG" ] && echo "mvn 日志: $LOG"
[ -d "$REPORTS_DIR" ] && echo "报告目录: $REPORTS_DIR"
echo "=========================================="
