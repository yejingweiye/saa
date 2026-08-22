# Spring AI Alibaba 可观测性示例
本示例演示如何将 **Spring AI Alibaba** 结合阿里云 ARMS，通过**阿里云 Java Agent** 实现应用可观测能力接入。

## 快速开始

### 前置条件
1. 获取 ARMS License Key：访问 [ARMS 控制台](https://arms.console.aliyun.com/)，开通服务并获取 ARMS license key。
2. 准备 Java Agent：下载 `aliyun-java-agent.jar`（本项目已预置在 [./src/javaagent/AliyunJavaAgent](./src/javaagent/AliyunJavaAgent) 目录下）。
> 如需获取最新版本 Agent 或技术支持，请提交 [阿里云工单](https://smartservice.console.aliyun.com/service/create-ticket) 联系技术团队。
3. 编译构建本项目。
4. [本地运行](#本地运行) Jar 包，启动时附加相关 JVM 参数。
5. 启动校验：控制台输出 `Started ObservabilityApplication in xxx seconds`，代表应用启动成功。
6. 调用示例接口
```bash
curl --location 'http://localhost:8080/joke'
```
7. [查看监控面板](https://arms.console.aliyun.com/#/llm/list/cn-hangzhou?from=now-15m&to=now&refresh=off)
   调用接口后，可在 ARMS 控制台查看采集到的 LLM 监控数据。数据上报延迟约1分钟；如果是新接入应用，资源初始化可能需要再多等待1分钟。

### 本地运行
```bash
mvn clean package -DskipTests

# 设置通义千问 API Key
export AI_DASHSCOPE_API_KEY=${AI_DASHSCOPE_API_KEY}

java \
  -javaagent:./src/javaagent/AliyunJavaAgent/aliyun-java-agent.jar \
  -Darms.licenseKey=${ARMS_LICENSE_KEY} \
  -Darms.appName=${ARMS_APP_NAME} \
  -Daliyun.javaagent.regionId=${ARMS_REGION_ID} \
  -jar ./target/observability-arms-example-1.0.0.jar
```

---

### 参数说明补充
| 参数 | 说明 |
|---|---|
| `-javaagent` | 指定阿里云 Java Agent jar 包路径 |
| `-Darms.licenseKey` | ARMS 授权密钥，控制台获取 |
| `-Darms.appName` | ARMS 内展示的应用名称，自定义填写 |
| `-Daliyun.javaagent.regionId` | ARMS 所在地域 ID，如 `cn-hangzhou` |
| `AI_DASHSCOPE_API_KEY` | 通义千问模型调用密钥 |

> 提示：
> 1. `mvn.test.skip=true` 旧写法，Maven3 推荐使用 `-DskipTests`；两者都可以生效。
> 2. 容器部署时，同样需要把 `-javaagent` 和上述系统参数配置到容器启动命令。
> 3. Agent 会自动埋点 Spring AI Alibaba 的大模型调用链路，无需业务代码手动埋点。

