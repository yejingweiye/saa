package com.yjw.mcp.sqlite.chatbot;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

/**
 * Spring AI + MCP SQLite 交互式聊天机器人示例
 * 功能：命令行交互式对话，大模型通过MCP访问SQLite数据库，支持多轮会话记忆
 * 输入 exit 退出程序
 */
@SpringBootApplication
public class MCPSqliteChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(MCPSqliteChatbotApplication.class, args);
    }

    /**
     * 交互式聊天入口，Spring启动完成后运行
     * @param chatClientBuilder 聊天客户端构建器
     * @param mcpClients MCP客户端列表，可以同时挂载多个MCP服务
     * @param context Spring应用上下文
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner interactiveChat(ChatClient.Builder chatClientBuilder,
                                             List<McpSyncClient> mcpClients,
                                             ConfigurableApplicationContext context) {
        return args -> {

            var chatClient = chatClientBuilder
                    // 将所有MCP服务暴露的工具注册给大模型
                    .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpClients))
                    // 开启窗口式聊天记忆，保存多轮对话上下文，支持上下文连续问答
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                    .build();

            // 读取控制台用户输入
            var scanner = new Scanner(System.in);
            System.out.println("\n启动交互式会话，输入 exit 即可退出程序。");

            try {
                // 循环接收用户输入
                while (true) {
                    System.out.print("\n用户: ");
                    String input = scanner.nextLine();

                    // 输入exit结束聊天循环
                    if (input.equalsIgnoreCase("exit")) {
                        System.out.println("结束聊天会话。");
                        break;
                    }

                    System.out.print("AI助手: ");
                    // 将用户自然语言传给大模型，模型可自动调用MCP SQLite工具执行SQL
                    System.out.println(chatClient.prompt(input).call().content());
                }
            } finally {
                // 关闭输入流，关闭Spring容器，自动销毁MCP子进程
                scanner.close();
                context.close();
            }

        };
    }


    /**
     * 创建MCP同步客户端Bean
     * destroyMethod = "close"：容器销毁时自动关闭MCP客户端以及uvx拉起的python子进程
     * @return McpSyncClient
     */
    @Bean(destroyMethod = "close")
    public McpSyncClient mcpClient() {

        // Windows环境需要把 uvx 修改为 uvx.exe
        // --with mcp<2 固定 Python mcp SDK 版本：最新 mcp-server-sqlite 与 mcp 2.x 不兼容（移除了 Server.list_resources），会导致 MCP 子进程启动即崩溃
        var stdioParams = ServerParameters.builder("uvx")
                .args("--with", "mcp<2", "mcp-server-sqlite", "--db-path", getDbPath())
                .build();

        // 创建同步MCP客户端，Stdio管道和sqlite MCP子进程通信，设置请求超时10秒
        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams, McpJsonMapper.getDefault()))
                .requestTimeout(Duration.ofSeconds(10)).build();

        // MCP协议初始化握手，建立会话
        var init = mcpClient.initialize();

        System.out.println("MCP初始化完成: " + init);

        return mcpClient;

    }

    /**
     * 获取SQLite数据库文件绝对路径
     * IDEA运行注意：需要核对工作目录(working dir)，否则会找不到test.db
     * @return test.db 文件路径
     */
    private static String getDbPath() {
//        return Paths.get(System.getProperty("user.dir"), "test.db").toString();
        return Paths.get(System.getProperty("user.dir"), "/saa-exmple/mcp/mcp-manual/sqlite/ai-mcp-sqlite-chatbot/test.db").toString();
    }

}