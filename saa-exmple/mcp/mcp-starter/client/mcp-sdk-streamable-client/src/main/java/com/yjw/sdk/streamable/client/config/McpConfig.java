package com.yjw.sdk.streamable.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * MCP 客户端配置
 * 执行流程：
 * 1. 创建客户端传输层 Transport
 * 2. 创建并初始化 MCP 异步客户端
 * 3. 定义MCP工具元数据（测试工具）
 * 4. 在 ChatClientConfig.java 中，包装工具，注册给大模型ChatClient
 * 5. 控制台交互，发送提问获取大模型回答
 */
@Configuration
public class McpConfig {

    @Value("${spring.ai.mcp.client.streamable.connections.server1.url}")
    private String mcpServerUrl;


    /**
     * 1. 创建STREAMABLE‑HTTP传输层Transport
     * 负责MCP协议报文收发、SSE流式通知接收
     */
    @Bean
    public WebClientStreamableHttpTransport mcpTransport(ObjectMapper objectMapper) {
        return WebClientStreamableHttpTransport.builder(WebClient.builder())
                .endpoint(mcpServerUrl)                // MCP服务端地址
                .resumableStreams(true)                // 开启流断点续传
                .objectMapper(objectMapper)            // JSON序列化对象
                .openConnectionOnStartup(true)         // 项目启动就建立连接，而非懒加载
                .build();
    }

    /**
     * 2. 创建并初始化MCP异步客户端
     * 基于STREAMABLE‑HTTP传输，提供工具调用、接收服务端推送通知能力
     */
    @Bean
    public McpAsyncClient mcpAsyncClient(WebClientStreamableHttpTransport transport) {
        return McpClient.async(transport).build();
    }

    /**
     * 对应Python MCP服务端示例：
     * examples/servers/simple-streamablehttp-stateless/mcp_simple_streamablehttp_stateless/server.py
     *
     * 3. 手动定义MCP工具元数据
     * 本地硬编码复刻远端服务端 start‑notification‑stream 工具定义，需和Python服务端list_tools返回保持一致
     * 工具作用：按照配置间隔持续推送多条通知消息，用于测试进度、日志通知推送
     */
    @Bean
    public McpSchema.Tool startNotificationTool() {
        String inputSchema = """
                    {
                      "type": "object",
                      "required": ["interval", "count", "caller"],
                      "properties": {
                        "interval": { "type": "number", "description": "通知间隔，单位秒" },
                        "count": { "type": "number", "description": "发送通知总条数" },
                        "caller": { "type": "string", "description": "调用方标识，携带在通知消息中" }
                      }
                    }
                """;
        return McpSchema.Tool.builder()
                .name("start-notification-stream")
                .description("可以按配置的间隔持续发送流式通知，可配置通知数量与间隔时间")
                .inputSchema(inputSchema)
                .build();
    }

    // 改造一个可以测试的tool 对应mcp-streamable-webflux-server  20000端口
    @Bean
    public McpSchema.Tool getCityTimeTool() {
        String inputSchema = """
        {
          "type": "object",
          "required": ["timeZoneId"],
          "properties": {
            "timeZoneId": {
              "type": "string",
              "description": "时区ID，例如 Asia/Shanghai"
            }
          }
        }
        """;

        return McpSchema.Tool.builder()
                .name("getCityTimeMethod")
                .description("获取指定城市的时间。")
                .inputSchema(inputSchema)
                .build();
    }

    /**
     * Jackson JSON序列化器，用于MCP协议报文序列化/反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}