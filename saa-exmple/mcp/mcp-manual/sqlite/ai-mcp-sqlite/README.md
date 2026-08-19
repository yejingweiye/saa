# Spring AI Model Context Protocol 演示应用
本演示项目展示 **Spring AI** 通过模型上下文协议（MCP）对接 SQLite 数据库。基于命令行实现用自然语言直接和 SQLite 数据库交互。

项目使用 [SQLite MCP‑Server](https://github.com/modelcontextprotocol/servers/tree/main/src/sqlite)，支持执行SQL查询、业务数据分析、自动生成业务洞察备忘录。

## 功能特性
- 使用自然语言查询 SQLite 数据库
- 预置问题模式，自动化完成数据库分析任务
- 无缝对接 OpenAI 大模型
- 基于 Spring AI 和 Model Context Protocol（MCP）协议构建

## 环境前置要求
- Java 17 及以上
- Maven 3.6+
- uvx 包管理工具
- Git
- OpenAI API Key
- SQLite（可选，用于手动修改数据库文件）

## 安装步骤

1. 安装 uvx（uv 通用包管理器）
> 参考官方文档完成安装：
https://docs.astral.sh/uv/getting-started/installation/

2. 克隆代码仓库
```bash
git clone https://github.com/spring-projects/spring-ai-examples.git
cd model-context-protocol/sqlite/simple
```

3. 配置 OpenAI API Key 环境变量
```bash
export OPENAI_API_KEY='你的api-key填在这里'
```

## SQLite示例数据库
SQLite数据库文件跨操作系统可直接使用。仓库自带示例数据库文件 `test.db`。
库内包含一张 `PRODUCTS` 商品表，数据库由脚本 `create-database.sh` 创建。

## 运行应用程序

### 执行预置问题模式
自动运行一组预设的数据库查询任务：
```bash
./mvnw spring-boot:run
```

## 架构总览
Spring AI 集成 MCP 的调用链路非常清晰：
1. **MCP 客户端**：底层通信层，负责和MCP服务端交互（这里就是SQLite MCP服务）
2. **函数回调（Function Callbacks）**：把MCP暴露出来的工具封装成大模型可以调用的函数
3. **ChatClient**：把封装好的函数回调绑定到大模型，完成完整调用链路

下面逐个讲解Bean定义，从ChatClient开始。

### ChatClient
```java
@Bean
@Profile("!chat")
public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
                                           List<McpFunctionCallback> functionCallbacks,
                                           ConfigurableApplicationContext context) {
    return args -> {
        var chatClient = chatClientBuilder.defaultFunctions(functionCallbacks.toArray(new McpFunctionCallback[0]))
                .build();
         // 运行预置提问
         System.out.println(chatClient.prompt(
            "Can you connect to my SQLite database and tell me what products are available, and their prices?").call().content());
         // ...省略其余问题
    };
}
```

ChatClient 的配置非常简洁，只需要传入从MCP工具转换出来的函数回调列表。Spring依赖注入自动完成全部装配，实现无缝集成。

接下来详细看其他Bean定义。

### 函数回调（Function Callbacks）
应用程序将MCP工具注册为Spring AI可识别的函数回调：
```java
@Bean
public List<McpFunctionCallback> functionCallbacks(McpSyncClient mcpClient) {
    return mcpClient.listTools(null)
            .tools()
            .stream()
            .map(tool -> new McpFunctionCallback(mcpClient, tool))
            .toList();
}
```

#### 作用
1. 从MCP客户端获取全部可用工具列表（工具发现）
2. 将每一个MCP工具转换为Spring AI的函数回调对象
3. 将回调提供给ChatClient供大模型调用

#### 执行流程
1. `mcpClient.listTools(null)`：向MCP服务查询所有可用工具
    - 参数`null`代表分页游标
    - 传`null`会返回第一页全部工具
    - 如果结果很多，可以传入游标字符串获取后续分页数据
2. `.tools()`：取出响应里面的工具集合
3. `.map()`：循环每个工具，包装成`McpFunctionCallback`回调对象
4. `.toArray()`：转为数组交给ChatClient

#### 使用效果
注册回调后，ChatClient具备这些能力：
- 在对话会话中访问MCP提供的全部工具
- 接收大模型下发的函数调用请求
- 代理执行MCP服务端工具（本案例就是SQLite数据库执行SQL）

### MCP客户端
本项目使用**同步MCP客户端**和SQLite MCP服务通信：
```java
@Bean(destroyMethod = "close")
public McpSyncClient mcpClient() {
    var stdioParams = ServerParameters.builder("uvx")
            .args("mcp-server-sqlite", "--db-path", getDbPath())
            .build();

    var mcpClient = McpClient.sync(new StdioServerTransport(stdioParams),
            Duration.ofSeconds(10), new ObjectMapper());

    var init = mcpClient.initialize();
    System.out.println("MCP Initialized: " + init);

    return mcpClient;
}
```

配置说明：
1. 创建基于标准输入输出（Stdio）的传输层，通过uvx拉起MCP子进程
2. 指定SQLite数据库后端和数据库文件路径
3. 设置操作超时时间10秒
4. 使用Jackson做JSON序列化解析MCP协议报文
5. 执行initialize完成MCP握手初始化连接

`destroyMethod = "close"`：应用关闭时自动释放资源、关闭子进程，避免残留进程。

## 参考文档
如果你需要阅读原版快速入门文档，可以访问这个github历史版本链接。

> 注意：在2024‑12‑10官方快速入门文档已经由SQLite示例改成了天气查询示例。
历史版本文档链接：
https://github.com/modelcontextprotocol/docs/blob/1024e03f83aa0b8badde9b50dfee4d2e4e7f9446/quickstart.mdx

你也可以基于此demo自行新建数据表，扩展SQLite业务场景。

---

### 补充小注释（便于你理解版本差异）
> 注意：你前面自己写的代码用的是 `SyncMcpToolCallbackProvider`，而官方老示例这里手写 `McpFunctionCallback`。
> - `SyncMcpToolCallbackProvider` 是后续封装的工具类，内部逻辑和这个示例手写stream转换是等价的，不用自己手动遍历tools做map转换。
> - 老示例代码里面 `StdioServerTransport` 在新版SDK改名为 `StdioClientTransport`。
