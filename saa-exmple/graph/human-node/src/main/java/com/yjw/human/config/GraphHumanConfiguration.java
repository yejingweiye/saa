package com.yjw.human.config;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.human.dispatcher.HumanFeedbackDispatcher;
import com.yjw.human.node.ExpanderNode;
import com.yjw.human.node.HumanFeedbackNode;
import com.yjw.human.node.TranslateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class GraphHumanConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GraphHumanConfiguration.class);

    @Bean
    public StateGraph humanGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("query", new ReplaceStrategy())
                .addPatternStrategy("thread_id", new ReplaceStrategy())
                .addPatternStrategy("expander_number", new ReplaceStrategy())
                .addPatternStrategy("expander_content", new ReplaceStrategy())
                .addPatternStrategy("feed_back", new ReplaceStrategy())
                .addPatternStrategy("human_next_node", new ReplaceStrategy())
                .addPatternStrategy("translate_language", new ReplaceStrategy())
                .addPatternStrategy("translate_content", new ReplaceStrategy())
                .build();

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("expander", node_async(new ExpanderNode(chatClientBuilder)))
                .addNode("translate", node_async(new TranslateNode(chatClientBuilder)))
                .addNode("human_feedback", node_async(new HumanFeedbackNode()))

                .addEdge(StateGraph.START, "expander")
                .addEdge("expander", "human_feedback")
                .addConditionalEdges("human_feedback", AsyncEdgeAction.edge_async((new HumanFeedbackDispatcher())), Map.of(
                        "translate", "translate", StateGraph.END, StateGraph.END))
                .addEdge("translate", StateGraph.END);

        // 添加 PlantUML 打印
        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                "human flow");
        logger.info("\n=== expander UML Flow ===");
        logger.info(representation.content());
        logger.info("==================================\n");

        return stateGraph;
    }
}
