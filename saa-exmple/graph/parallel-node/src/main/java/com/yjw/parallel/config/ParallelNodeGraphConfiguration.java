package com.yjw.parallel.config;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.parallel.dispatcher.CollectorDispatcher;
import com.yjw.parallel.node.CollectorNode;
import com.yjw.parallel.node.DispatcherNode;
import com.yjw.parallel.node.ExpanderNode;
import com.yjw.parallel.node.TranslateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class ParallelNodeGraphConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ParallelNodeGraphConfiguration.class);


    @Bean
    public StateGraph parallelNodeGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("query", new ReplaceStrategy())
                .addPatternStrategy("expander_number", new ReplaceStrategy())
                .addPatternStrategy("expander_content", new ReplaceStrategy())
                .addPatternStrategy("translate_language", new ReplaceStrategy())
                .addPatternStrategy("translate_content", new ReplaceStrategy())
                .addPatternStrategy("collector_next_node", new ReplaceStrategy())
                .addPatternStrategy("expand_status", new ReplaceStrategy())
                .addPatternStrategy("translate_status", new ReplaceStrategy())
                .build();

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("dispatcher", node_async(new DispatcherNode()))
                .addNode("translator", node_async(new TranslateNode(chatClientBuilder)))
                .addNode("expander", node_async(new ExpanderNode(chatClientBuilder)))
                .addNode("collector", node_async(new CollectorNode()))

                // 并行边
                .addEdge(StateGraph.START, "dispatcher")
                .addEdge("dispatcher", "translator")
                .addEdge("dispatcher", "expander")
                .addEdge("translator", "collector")
                .addEdge("expander", "collector")
                .addConditionalEdges("collector", edge_async(new CollectorDispatcher()),
                        Map.of("dispatcher", "dispatcher", END, END));

        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                "parallel translator and expander flow");
        logger.info("\n=== Parallel Translator and Expander UML Flow ===");
        logger.info(representation.content());
        logger.info("==================================\n");

        return stateGraph;


    }
}
