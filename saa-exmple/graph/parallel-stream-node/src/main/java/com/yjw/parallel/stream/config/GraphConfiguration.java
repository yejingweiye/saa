package com.yjw.parallel.stream.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.parallel.stream.model.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.yjw.parallel.stream.node.*;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;


@Configuration
public class GraphConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GraphConfiguration.class);

    @Bean
    public StateGraph parallelStreamGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("query", new ReplaceStrategy())
                .addPatternStrategy("expander_number", new ReplaceStrategy())
                .addPatternStrategy("expander_content", new ReplaceStrategy())
                .addPatternStrategy("translate_language", new ReplaceStrategy())
                .addPatternStrategy("translate_content", new ReplaceStrategy())
                .addPatternStrategy("merge_result", new ReplaceStrategy())
                .build();

        Map<String, NodeStatus> node2Status = new HashMap<>();

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode(ExpanderNode.NODE_NAME, node_async(new ExpanderNode(chatClientBuilder, node2Status)))
                .addNode(TranslateNode.NODE_NAME, node_async(new TranslateNode(chatClientBuilder, node2Status)))
                .addNode(MergeResultsNode.NODE_NAME, node_async(new MergeResultsNode(node2Status)))

                .addEdge(StateGraph.START, TranslateNode.NODE_NAME)
                .addEdge(StateGraph.START, ExpanderNode.NODE_NAME)

                .addEdge(TranslateNode.NODE_NAME, MergeResultsNode.NODE_NAME)
                .addEdge(ExpanderNode.NODE_NAME, MergeResultsNode.NODE_NAME)

                .addEdge(MergeResultsNode.NODE_NAME, StateGraph.END);

        // 添加 PlantUML 打印
        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                "expander flow");
        logger.info("\n=== expander UML Flow ===");
        logger.info(representation.content());
        logger.info("==================================\n");

        return stateGraph;
    }

    private class MergeResultsNode implements NodeAction {

        public static final String NODE_NAME = "merge";

        private final Map<String, NodeStatus> node2Status;

        public MergeResultsNode(Map<String, NodeStatus> node2Status) {
            this.node2Status = node2Status;
        }

        @Override
        public Map<String, Object> apply(OverAllState state) {
            if (!isDone(node2Status)) { // 扩展节点和翻译节点状态有一方不是COMPLETED
                return Map.of();
            }

            Object expanderContent = state.value("expander_content").orElse("unknown");
            String translateContent = (String) state.value("translate_content").orElse("");

            return Map.of("merge_result", Map.of("expander_content", expanderContent,
                    "translate_content", translateContent));
        }

        private boolean isDone(Map<String, NodeStatus> node2Status) {
            return node2Status.get(ExpanderNode.NODE_NAME) == NodeStatus.COMPLETED
                    && node2Status.get(TranslateNode.NODE_NAME) == NodeStatus.COMPLETED;
        }
    }
}
