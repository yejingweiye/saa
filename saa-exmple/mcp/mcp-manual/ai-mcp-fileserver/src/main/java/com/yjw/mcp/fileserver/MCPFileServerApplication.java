package com.yjw.mcp.fileserver;

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

/**
 * Spring AI + MCP 文件系统客户端示例
 * 功能：大模型通过MCP协议，自动调用文件读写工具，读写本机指定目录文件
 */
@SpringBootApplication
public class MCPFileServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MCPFileServerApplication.class, args);
    }

    /**
     * stdio 运行MCP文件服务子进程，然后调用MCP工具
     *
     */
    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
                                                 McpSyncClient mcpClient,
                                                 ConfigurableApplicationContext context) {

        return args -> {
            // 将MCP提供的全部工具注册到ChatClient，大模型可自动调用MCP工具
            var chatClient = chatClientBuilder
                    .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpClient))
                    .build();

            System.out.println("开始执行预设问题，等待AI模型返回结果:\n");

            // 问题1：读取并解释本地文本文件内容
            String question1 = "请解释 target/spring-ai-mcp-overview.txt 文件里面的内容？";
            System.out.println("用户提问: " + question1);
            // 发起调用，大模型会自动调用MCP读文件工具读取本地文件再回答
            System.out.println("AI回答: " + chatClient.prompt(question1).call().content());
            System.out.println("===========================================================");

            // 问题2：读取文件，总结内容，将总结结果写入本地markdown文件
            String question2 = "请读取 target/spring-ai-mcp-overview.txt 文件，总结里面的内容，以Markdown格式保存到 target/summary.md 文件中";
            System.out.println("\n用户提问: " + question2);
            // 大模型会先后调用读取文件、写入文件两个MCP工具完成任务
            System.out.println("AI回答: " + chatClient.prompt(question2).call().content());

            // 执行完毕关闭Spring容器，程序退出
            context.close();
        };
    }

    /**
     * 创建MCP同步客户端Bean
     * destroyMethod = close：Spring容器销毁时自动关闭MCP客户端与子进程
     * @return McpSyncClient
     */
    @Bean(destroyMethod = "close")
    public McpSyncClient mcpClient() {

        // 启动官方MCP文件服务子进程
        // Windows系统把 "npx" 修改为 "npx.cmd"，否则无法拉起子进程
        var stdioParams = ServerParameters.builder("npx")
                .args("-y", "@modelcontextprotocol/server-filesystem", getDbPath())
                .build();

        // 创建MCP同步客户端，使用标准输入输出(stdio)和MCP文件服务通信，设置请求超时10秒
        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams,
                        McpJsonMapper.getDefault()))
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        // 执行MCP初始化握手，建立会话
        var init = mcpClient.initialize();
        System.out.println("MCP客户端初始化完成: " + init);

        return mcpClient;
    }

    /**
     * 获取MCP文件服务允许访问的根目录
     * MCP文件服务器只能读写该目录及其子目录，不能访问电脑其他路径，属于安全限制
     * @return 本地文件目录绝对路径
     */
    private static String getDbPath() {
        // 拼接项目target目录绝对路径
        String path = Paths.get(System.getProperty("user.dir"),
                        "/saa-exmple/mcp/mcp-manual/ai-mcp-fileserver/target")
                .toString();
        System.out.println("MCP允许访问的文件根目录：" + path);
        return path;
    }

}