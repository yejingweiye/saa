package com.yjw.mcp.node.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.yjw.mcp.node.tool.McpClientToolCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class McpNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(McpNode.class);

    private static final String NODE_NAME = "mcp-node";

    private final ChatClient chatClient;


    public McpNode(ChatClient.Builder chatClientBuilder, McpClientToolCallbackProvider mcpClientToolCallbackProvider){

        Set<ToolCallback> toolCallbacks = mcpClientToolCallbackProvider.findToolCallbacks(NODE_NAME);
        for (ToolCallback toolCallback : toolCallbacks) {
            logger.info("Mcp Node load ToolCallback: " + toolCallback.getToolDefinition().name());
        }

        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolCallbacks.toArray(ToolCallback[]::new))
                .build();
    }


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String query = state.value("query","");
        Flux<String> streamResult = chatClient.prompt(query).stream().content();
        // 流拼接成一整段返回
        String result = streamResult.reduce("",(acc,item)->acc+item).block();

        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("mcp_content", result);
        return resultMap;
    }
}
