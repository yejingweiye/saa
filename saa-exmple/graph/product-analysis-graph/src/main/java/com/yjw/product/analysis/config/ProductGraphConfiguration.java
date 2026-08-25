package com.yjw.product.analysis.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.product.analysis.model.Product;
import com.yjw.product.analysis.serializer.ProductStateSerializer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class ProductGraphConfiguration {

    @Bean
    public StateGraph productAnalysisGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException{
        ChatClient client = chatClientBuilder.build();

        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("productDesc", new ReplaceStrategy())
                .addPatternStrategy("slogan", new ReplaceStrategy())
                .addPatternStrategy("productSpec", new ReplaceStrategy())
                .addPatternStrategy("finalProduct", new ReplaceStrategy())
                .build();

        // 创建自定义序列化器，用于处理 Product 对象的序列化
        AgentStateFactory<OverAllState>  stateFactory = OverAllState::new;
        ProductStateSerializer serializer = new ProductStateSerializer(stateFactory);

        NodeAction marketingCopyNode = state -> {
            String productDesc = (String) state.value("productDesc").orElseThrow();
            String slogan = client.prompt()
                    .user("根据以下产品描述，为产品生成一句朗朗上口的广告标语：" + productDesc)
                    .call()
                    .content();
            return Map.of("slogan", slogan);
        };

        NodeAction specificationExtractionNode = state -> {
            String productDesc = (String) state.value("productDesc").orElseThrow();
            Product productSpec = client.prompt()
                    .user("从以下产品描述中提取产品规格信息：" + productDesc)
                    .call()
                    .entity(Product.class);
            return Map.of("productSpec", productSpec);
        };

        NodeAction mergeNode = state -> {
            String slogan = (String) state.value("slogan").orElseThrow();
            Product productSpec = (Product) state.value("productSpec").orElseThrow();
            Product finalProduct = new Product(slogan, productSpec.material(), productSpec.colors(), productSpec.season());
            return Map.of("finalProduct", finalProduct);
        };

        StateGraph graph = new StateGraph(keyStrategyFactory, serializer);
        graph.addNode("marketingCopy", node_async(marketingCopyNode))
                .addNode("specificationExtraction", node_async(specificationExtractionNode))
                .addNode("merge", node_async(mergeNode))
                .addEdge(START, "marketingCopy")
                .addEdge(START, "specificationExtraction")
                .addEdge("marketingCopy", "merge")
                .addEdge("specificationExtraction", "merge")
                .addEdge("merge", END);

        GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "产品分析流程图");
        System.out.println("\n=== 产品分析流程 UML 图 ===");
        System.out.println(representation.content());
        System.out.println("======================================\n");

        return graph;



    }
}
