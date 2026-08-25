package com.yjw.classifier.graph;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.yjw.classifier.node.HttpNode;
import com.yjw.classifier.node.QuestionClassifierNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

@Component
public class GraphBuilder {

    @Bean
    public CompiledGraph buildGraph(ChatModel chatModel) throws GraphStateException {
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("input", (o1, o2) -> o2)
                // 业务固定output key，不再写时间戳
                .addPatternStrategy("first_classify_output", (o1, o2) -> o2)
                .addPatternStrategy("second_classify_output", (o1, o2) -> o2)
                .addPatternStrategy("http_negative_output", (o1, o2) -> o2)
                .addPatternStrategy("http_positive_output", (o1, o2) -> o2)
                .build();

        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        // 一级情绪分类：正向/负向反馈
        QuestionClassifierNode firstClassifier = QuestionClassifierNode.builder()
                .chatClient(chatClient)
                .inputTextKey("input")
                .categories(List.of("positive feedback", "negative feedback"))
                .outputKey("first_classify_output")
                .classificationInstructions(List.of("请根据输入内容选择对应分类"))
                .build();
        stateGraph.addNode("first_classify", AsyncNodeAction.node_async(firstClassifier));

        // 二级负向分类：售后 / 产品质量
        QuestionClassifierNode secondNegativeClassifier = QuestionClassifierNode.builder()
                .chatClient(chatClient)
                .inputTextKey("input")
                .categories(List.of("after‑sale service", "product quality"))
                .outputKey("second_classify_output")
                .classificationInstructions(List.of("请根据输入内容选择对应分类"))
                .build();
        stateGraph.addNode("second_negative_classify", AsyncNodeAction.node_async(secondNegativeClassifier));

        // 调用负向http接口
        HttpNode httpNegative = HttpNode.builder()
                .url("http://127.0.0.1:18080/negative")
                .header("Content-Type", "application/json")
                .retryConfig(new HttpNode.RetryConfig(3, 100, true))
                .outputKey("http_negative_output")
                .build();
        stateGraph.addNode("call_negative_http", AsyncNodeAction.node_async(httpNegative));

        // 调用正向http接口
        HttpNode httpPositive = HttpNode.builder()
                .url("http://127.0.0.1:18080/positive")
                .header("Content-Type", "application/json")
                .retryConfig(new HttpNode.RetryConfig(3, 100, true))
                .outputKey("http_positive_output")
                .build();
        stateGraph.addNode("call_positive_http", AsyncNodeAction.node_async(httpPositive));

        // edges
        stateGraph.addEdge(START, "first_classify");
        stateGraph.addEdge("call_negative_http", END);
        stateGraph.addEdge("call_positive_http", END);

        // 一级分类条件分支
        stateGraph.addConditionalEdges("first_classify",
                edge_async(state -> {
                    String value = state.value("first_classify_output", String.class).orElse("");
                    if (value.contains("negative feedback")) return "negative feedback";
                    if (value.contains("positive feedback")) return "positive feedback";
                    return null;
                }),
                Map.of(
                        "negative feedback", "second_negative_classify",
                        "positive feedback", "call_positive_http"
                )
        );

        // 二级分类条件分支：售后、产品质量都走负向接口
        stateGraph.addConditionalEdges("second_negative_classify",
                edge_async(state -> {
                    String value = state.value("second_classify_output", String.class).orElse("");
                    if (value.contains("after‑sale service")) return "after‑sale service";
                    if (value.contains("product quality")) return "product quality";
                    return null;
                }),
                Map.of(
                        "after‑sale service", "call_negative_http",
                        "product quality", "call_negative_http"
                )
        );

        printGraphImage(stateGraph);
        return stateGraph.compile();
    }

    private static void printGraphImage(StateGraph stateGraph) {
        GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                "workflow graph");
        System.out.println("\n\n");
        System.out.println(graphRepresentation.content());
        System.out.println("\n\n");
    }
}

