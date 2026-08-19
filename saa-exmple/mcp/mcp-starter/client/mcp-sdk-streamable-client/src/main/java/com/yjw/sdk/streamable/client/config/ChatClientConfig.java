
package com.yjw.sdk.streamable.client.config;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    /**
     * 原来只可以添加一个tool: startNotificationTool
     */
    @Bean
    public AsyncMcpToolCallback mcpToolCallback(
            McpAsyncClient mcpAsyncClient,
            McpSchema.Tool startNotificationTool
    ) {
        return new AsyncMcpToolCallback(mcpAsyncClient, startNotificationTool);
    }

    /**
     * 可以添加多个tool
     * 改造后的添加
     */
    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            McpAsyncClient mcpAsyncClient,
            // 注入多个McpSchema.Tool Bean
            McpSchema.Tool startNotificationTool,
            McpSchema.Tool getCityTimeTool
    ) {
        // 循环/构造多个Callback
        var callback1 = new AsyncMcpToolCallback(mcpAsyncClient, startNotificationTool);
        var callback2 = new AsyncMcpToolCallback(mcpAsyncClient, getCityTimeTool);

        return ToolCallbackProvider.from(callback1, callback2);
    }


}
