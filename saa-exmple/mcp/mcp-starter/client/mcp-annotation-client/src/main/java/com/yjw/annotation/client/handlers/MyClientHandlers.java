package com.yjw.annotation.client.handlers;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springframework.stereotype.Component;

@Component
public class MyClientHandlers {
    private static final Logger logger = LoggerFactory.getLogger(MyClientHandlers.class);

    // 接收 MCP 服务端主动推送的日志通知 LoggingMessageNotification
    @McpLogging(clients = "server1")
    public void handleLogs(McpSchema.LoggingMessageNotification notification) {
        // Handle logs
        logger.info("Logs: {}", notification.data());
    }

    // 接收抽样通知 CreateMessageResult
    @McpSampling(clients = "server1")
    public McpSchema.CreateMessageResult handleSampling(McpSchema.CreateMessageRequest request) {
        // Handle sampling
        logger.info("Sampling: {}", request.messages());
        return null;
    }

    // 接收 进度通知 ProgressNotification
    @McpProgress(clients = "server1")
    public void handleProgress(McpSchema.ProgressNotification notification) {
        // Handle progress
        logger.info("Progress: {}", notification);
    }


}
