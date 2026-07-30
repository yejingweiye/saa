package com.yjw.config;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.node.FinalProcessNode;
import com.yjw.node.OrderApprovalNode;
import com.yjw.node.SensitiveOperationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class InterruptableGraphConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(InterruptableGraphConfiguration.class);

    @Bean(name = "orderApprovalGraph")
    public StateGraph orderApprovalGraph() throws GraphStateException {
        logger.info("Initializing orderApprovalGraph");

        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .defaultStrategy(new ReplaceStrategy())
                .addStrategy("order_id")
                .addStrategy("order_amount")
                .addStrategy("approved")
                .addStrategy("order_status")
                .addStrategy("message")
                .addStrategy("processed_time")
                .addStrategy("workflow_status")
                .addStrategy("summary")
                .build();

        OrderApprovalNode orderApprovalNode = new OrderApprovalNode();
        FinalProcessNode finalProcessNode = new FinalProcessNode();

        StateGraph graph = new StateGraph(keyStrategyFactory)
                .addNode("order_approval", orderApprovalNode)
                .addNode("final_process", finalProcessNode)
                .addEdge(StateGraph.START, "order_approval")
                .addEdge("order_approval", "final_process")
                .addEdge("final_process", StateGraph.END);

        printGraphRepresentation(graph, "订单审批工作流");

        logger.info("orderApprovalGraph initialized");
        return graph;
    }

    @Bean(name = "sensitiveOperationGraph")
    public StateGraph sensitiveOperationGraph() throws GraphStateException {
        logger.info("Initializing sensitiveOperationGraph");

        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .defaultStrategy(new ReplaceStrategy())
                .addStrategy("operation")
                .addStrategy("operation_params")
                .addStrategy("status")
                .addStrategy("result")
                .addStrategy("error")
                .addStrategy("executed_time")
                .addStrategy("workflow_status")
                .addStrategy("summary")
                .build();

        SensitiveOperationNode sensitiveOperationNode = new SensitiveOperationNode();
        FinalProcessNode finalProcessNode = new FinalProcessNode();
        StateGraph graph = new StateGraph(keyStrategyFactory)
                .addNode("sensitive_operation", sensitiveOperationNode)
                .addNode("final_process", finalProcessNode)
                .addEdge(StateGraph.START, "sensitive_operation")
                .addEdge("sensitive_operation", "final_process")
                .addEdge("final_process", StateGraph.END);

        printGraphRepresentation(graph, "敏感操作确认工作流");
        logger.info("sensitiveOperationGraph initialized");
        return graph;
    }

    private void printGraphRepresentation(StateGraph graph, String graphName) {
        try {
            GraphRepresentation representation = graph.getGraph(
                    GraphRepresentation.Type.PLANTUML,
                    graphName
            );
            logger.info("\n========== {} ==========\n{}\n====================\n",
                    graphName, representation.content());
        } catch (Exception e) {
            logger.warn("Failed to generate graph representation: {}", e.getMessage());
        }
    }
}