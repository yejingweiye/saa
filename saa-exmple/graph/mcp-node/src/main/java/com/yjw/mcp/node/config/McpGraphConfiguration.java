package com.yjw.mcp.node.config;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.mcp.node.node.McpNode;
import com.yjw.mcp.node.tool.McpClientToolCallbackProvider;
import io.modelcontextprotocol.client.McpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;


@Configuration
@EnableConfigurationProperties({McpNodeProperties.class}) // 注册为bean
public class McpGraphConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpGraphConfiguration.class);

    @Autowired
    private McpClientToolCallbackProvider mcpClientToolCallbackProvider;

    @Bean
    public StateGraph mcpGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("query",new ReplaceStrategy())
                .addPatternStrategy("mcp_content",new ReplaceStrategy())
                .build();

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("mcp",node_async(new McpNode(chatClientBuilder, mcpClientToolCallbackProvider)))

                .addEdge(StateGraph.START, "mcp")
                .addEdge("mcp", StateGraph.END);

        // 添加 PlantUML 打印
        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                "mcp flow");
        logger.info("\n=== mcp UML Flow ===");
        logger.info(representation.content());
        logger.info("==================================\n");

        return stateGraph;
    }


}
