---
name: unit-test
description: 为Java类生成SpringBootTest单元测试，执行mvn test并输出测试报告。调用：/unit-test [目标]，目标可为文件路径、全限定类名、类名、包路径，不传则处理当前打开文件。自动定位模块与@SpringBootApplication主类、用@MockitoBean mock外部依赖(LLM/DashScope/网络)、运行测试、输出surefire测试报告。
disable-model-invocation: true
---

# 技能：单元测试生成与执行

## 目标
为 SpringBoot Java 类生成 **@SpringBootTest 集成测试**，执行测试并输出**测试报告**。
完整流程：**分析被测类 → 生成测试类 → 执行 mvn test → 输出 surefire 测试报告**。

## 调用输入
1. `/unit-test`：处理当前打开文件
2. `/unit-test src/main/java/com/xxx/service/OrderService.java`：指定文件（仓库根相对路径）
3. `/unit-test com.yjw.service.OrderService`：全限定类名
4. `/unit-test OrderService`：简单类名（自动搜索）
5. `/unit-test src/main/java/com/xxx/service/`：目录（逐文件处理）

## 执行流程

### 阶段一：分析被测类
- 读取目标类源码，梳理：
  - public 方法签名、入参、返回值
  - 依赖（构造器注入 / `@Autowired` 字段注入的 Bean）
  - 关键分支（if-else、异常抛出、集合/空值边界）
- 定位所属 Maven 模块：从源码文件向上找到最近的 `pom.xml`。
- 定位模块内 `@SpringBootApplication` 主类（供 `@SpringBootTest` 使用）。
- 检查模块 `pom.xml` 是否含 `spring-boot-starter-test`（test scope）；**缺失则添加**该依赖。
- 识别外部依赖并记录待 mock 清单：`ChatClient`/LLM、DashScope、HTTP、文件、定时器等。

### 阶段二：生成测试类
- 测试文件位置：`<模块>/src/test/java/<包路径>/<类名>Test.java`（与源码同包名）。
- 注解规范：
  - `@SpringBootTest`（能自动发现主类就裸用；否则 `@SpringBootTest(classes = XxxApplication.class)`）
  - Controller 测试：追加 `@AutoConfigureMockMvc` + `@Autowired MockMvc`，用 MockMvc 调接口断言 HTTP 状态与返回体。
  - Service/Node 测试：`@Autowired` 注入被测 Bean，直接调用方法断言结果。
  - **用 `@MockitoBean`（Spring Boot 3.4+，本项目 3.5.7 支持）mock 所有 LLM/外部 Bean**，保证不联网、不需要 API Key 也能启动上下文；老版本兜底用 `@MockBean`。
- 用例设计覆盖：正常路径、空值/边界入参、异常路径、关键分支；断言用 AssertJ `assertThat`。
- LLM/网络相关方法一律 `when(...).thenReturn(固定值)`，**禁止真实调用**。
- 方法命名：`testXxx_场景`，如 `testCreateOrder_Success`、`testCreateOrder_WhenStockNull_Throws`。
- **退化规则**：若被测类是纯 POJO/静态工具类（无 Spring Bean），或模块 classpath 含无法启动的重型自动配置（Redis/DB/LLM/Nacos 等）会导致 `@SpringBootTest` 上下文启动失败，则退化为**纯 JUnit 5 测试**（无需 Spring 注解），保证测试能真实运行。

### 阶段三：执行测试
优先调用辅助脚本（自动定位模块、执行、聚合报告）：
```bash
bash .claude/skills/unit-test/scripts/run-tests.sh <被测类文件路径|FQCN|类名>
```
脚本实际在**被测模块目录内**执行（本仓库根 reactor 存在损坏 pom，从根目录 `-pl/-am` 会整体失败）：
```bash
cd <模块相对路径>
mvn test -Dtest=<测试类简单名> -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false
```
> 若依赖兄弟模块且本地仓库未安装，先对依赖模块 `mvn install -DskipTests`。
> 若编译失败或首次拉依赖较慢，如实说明，不要静默跳过。

### 阶段四：输出测试报告
严格按照下面**固定结构**输出，不要删减模块。

#### 1. 被测类与测试类
- 被测类全限定名、测试类全限定名、所在模块、测试文件路径。

#### 2. 生成的测试用例清单
- 每个测试方法一行：方法名 + 覆盖场景 + 关键断言。表格形式。

#### 3. 执行结果汇总
- `Tests run / Failures / Errors / Skipped`、`BUILD SUCCESS/FAILURE`、构建耗时（如可得）。

#### 4. 失败用例详情
- 每个失败用例：`类名#方法名` + 断言/异常信息（从 surefire `.txt`/`.xml` 提取关键堆栈）。
- 失败必须**修复测试或说明原因后重跑**，直到全部通过或给出合理解释。

#### 5. 覆盖情况
- 被测类的主要 public 方法/分支是否被覆盖；明确列出**未覆盖**的方法与原因。

#### 6. 遗留风险与建议
- 依赖 mock 导致的未覆盖逻辑、需要真实环境才能测的链路、补充建议。

## 规则
1. 测试必须**确定性**：不用真实 LLM、不 `sleep`、不用随机值、不依赖真实时钟/网络。
2. 只新增测试代码；**不修改被测类业务逻辑**（确需微调时先说明）。
3. 找不到主类、缺依赖、编译失败，如实报告，不编造通过。
4. 测试文件生成后必须实际执行，禁止只贴代码不跑。
5. 若模块本身编译失败（仓库既有问题），如实指出，不强行绕过或编造报告。

## 禁止
- 禁止编写真实调用 DashScope/OpenAI/外部 HTTP 的测试（会挂起且消耗 token）。
- 禁止因测试失败而删测试、跳过验证、谎报通过。
- 禁止伪造覆盖情况。
