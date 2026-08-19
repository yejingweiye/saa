package com.yjw.mcp.sqlite;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Spring AI + MCP SQLite 示例
 * 功能：大模型通过MCP协议自动调用SQLite数据库工具，执行查询、统计、建表等数据库操作
 * 依赖uvx运行mcp‑server‑sqlite，需要本机安装uv工具
 */
@SpringBootApplication
public class MCPSqlitApplication {

    public static void main(String[] args) {
        SpringApplication.run(MCPSqlitApplication.class, args);
    }

    /**
     * Spring容器启动完成后自动执行，运行预设自然语言提问
     * @param chatClientBuilder SpringAI聊天客户端构建器
     * @param mcpClients MCP同步客户端列表，可以注入多个MCP客户端，同时使用多组工具
     * @param context Spring应用上下文
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
                                                 List<McpSyncClient> mcpClients,
                                                 ConfigurableApplicationContext context) {

        return args -> {
            // 将全部MCP客户端暴露的工具注册给ChatClient，大模型自动发现并调用数据库工具
            var chatClient = chatClientBuilder
                    .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpClients))
                    .build();

            System.out.println("开始执行预设问题，等待AI模型返回结果:\n");

            // 问题1：查询SQLite库中的商品和价格，AI自动执行SQL查询
            String question1 = "请连接SQLite数据库，告诉我有哪些商品以及对应的价格？";
            System.out.println("用户提问: " + question1);
            System.out.println("AI回答: " + chatClient.prompt(question1).call().content());

            // 问题2：统计所有商品平均价格
            String question2 = "数据库里面全部商品的平均价格是多少？";
            System.out.println("\n用户提问: " + question2);
            System.out.println("AI回答: " + chatClient.prompt(question2).call().content());

            // 问题3：价格分布分析，给出定价优化建议
            String question3 = "帮我分析商品的价格分布，给出一些定价优化建议";
            System.out.println("\n用户提问: " + question3);
            System.out.println("AI回答: " + chatClient.prompt(question3).call().content());

            // 问题4：设计并创建客户订单数据表，AI自动生成DDL建表语句执行
            String question4 = "帮我设计并新建一张用于存储客户订单的数据表";
            System.out.println("\n用户提问: " + question4);
            System.out.println("AI回答: " + chatClient.prompt(question4).call().content());

            System.out.println("\n所有预设问题执行完毕，即将退出程序。");
            // 关闭Spring容器，程序结束，同时会自动关闭MCP子进程
            context.close();
        };
    }

    /**
     * 创建MCP同步客户端Bean
     * destroyMethod = close：Spring销毁Bean时自动关闭MCP客户端以及uvx拉起的子进程
     * @return McpSyncClient MCP同步客户端
     */
    @Bean(destroyMethod = "close")
    public McpSyncClient mcpClient() {

        // uvx：uv工具提供的执行器，类似npx，用来运行python编写的mcp‑server‑sqlite服务
        // Windows注意：部分环境需要改为 uvx.exe
        var stdioParams = ServerParameters.builder("uvx")
                // --with mcp<2 固定 Python mcp SDK 版本：最新 mcp-server-sqlite 与 mcp 2.x 不兼容（移除了 Server.list_resources），会导致 MCP 子进程启动即崩溃
                .args("--with", "mcp<2", "mcp-server-sqlite", "--db-path", getDbPath())
                .build();

        // 创建MCP同步客户端，使用Stdio管道和sqlite MCP子进程通信，设置请求超时10秒
        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams, McpJsonMapper.getDefault()))
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        // MCP协议初始化握手，和子进程建立会话
        var init = mcpClient.initialize();
        System.out.println("MCP‑SQLite客户端初始化完成: " + init);

        return mcpClient;
    }

    /**
     * 获取SQLite数据库文件绝对路径
     * @return test.db数据库文件路径
     */
    private static String getDbPath() {
        String path = Paths.get(System.getProperty("user.dir"), "test.db").toString();
        System.out.println("SQLite数据库文件路径：" + path);
        return path;
    }

}