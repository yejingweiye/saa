package com.yjw.gol.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.google.common.collect.Lists;
import com.yjw.gol.node.ChatNode;
import com.yjw.gol.node.MergeNode;
import com.yjw.gol.node.SimpleSubGraph;
import com.yjw.gol.node.StreamingChatNode;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;


/**
 * 图配置类
 * <p>
 * 配置具备可观测能力的业务图，包含多种节点与边类型：
 * - 起始节点：初始预处理
 * - 并行节点：并发执行情感分析与主题分析
 * - 子图节点：内部串行业务处理
 * - 流式节点：AI实时响应流式输出
 * - 汇总节点：多结果聚合整理
 * - 结束节点：最终输出格式化
 *
 */

@Configuration
public class GraphConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GraphConfiguration.class);

    @Bean
    public RestClient.Builder createRestClient() {

        // 1. 创建 RequestConfig 并设置超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(10, TimeUnit.MINUTES))
                .setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
                .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES))
                .build();

        // 2. 创建 CloseableHttpClient 并应用配置
        HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

        // 3. 使用 HttpComponentsClientHttpRequestFactory 包装 HttpClient
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // 4. 创建 RestClient 并设置请求工厂
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * 配置ChatClient，集成日志打印顾问
     *
     * @param chatModel 大模型聊天实例
     * @return 配置完成的ChatClient对象
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * Configure the observability graph
     *
     * @param chatClient the chat client for AI processing
     * @return configured StateGraph
     * @throws GraphStateException if graph configuration fails
     *                             <p>
     *                             总图：
     *                             开始节点(input,start_output)
     *                             ->并行节点(情感分析[start_output, parallel_output1], 主题分析[start_output, parallel_output2])
     *                             ->合并节点(parallel_output1+parallel_output2，sub_input)
     *                             ->子图节点(subgraph_input, subgraph_final_output)
     *                             ->流式节点(subgraph_final_output, streaming_output)
     *                             ->汇总节点(streaming_output, summary_output)
     *                             ->结束节点(summary_output，end_output)
     *                             <p>
     *                             子图：
     *                             子图节点1（sub_input，sub_output1）
     *                             ->子图节点2（sub_output1，sub_output2）
     *                             ->子图节点3（sub_output2，subgraph_final_output）
     */
    @Bean
    public StateGraph observabilityGraph(ChatClient chatClient) throws GraphStateException {
        // Start node - initial processing
        ChatNode startNode = ChatNode.create("StartNode", "input", "start_output", chatClient,
                "Please perform initial processing on the input content:");


        // Parallel nodes - concurrent processing 情感分析
        ChatNode parallelNode1 = ChatNode.create("ParallelNode1", "start_output", "parallel_output1", chatClient,
                "Please perform sentiment analysis on the content:");
        ChatNode parallelNode2 = ChatNode.create("ParallelNode2", "start_output", "parallel_output2", chatClient,
                "Please perform topic analysis on the content:");


        // Merge node - combine parallel outputs for subgraph input
        // 合并节点没传chatClient
        MergeNode mergeNode = new MergeNode(Lists.newArrayList("parallel_output1", "parallel_output2"), "sub_input");

        // Streaming node - real-time AI response
        StreamingChatNode streamingNode = StreamingChatNode.create("StreamingNode", "subgraph_final_output", "streaming_output",
                chatClient, "Please perform detailed analysis on the subgraph results:");

        // Summary node - aggregates streaming output
        ChatNode summaryNode = ChatNode.create("SummaryNode", "streaming_output", "summary_output", chatClient,
                "Please summarize the streaming analysis results:");

        // End node - final output formatting
        ChatNode endNode = ChatNode.create("EndNode", "summary_output", "end_output", chatClient,
                "Please format the final results for output:");

        // Create subgraph
        SimpleSubGraph subGraph = new SimpleSubGraph(chatClient);

        // Define key strategies for state management
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("input", new ReplaceStrategy())
                .addPatternStrategy("start_output", new ReplaceStrategy())
                .addPatternStrategy("parallel_output1", new ReplaceStrategy())
                .addPatternStrategy("parallel_output2", new ReplaceStrategy())
                .addPatternStrategy("sub_input", new ReplaceStrategy())
                .addPatternStrategy("sub_output1", new ReplaceStrategy())
                .addPatternStrategy("sub_output2", new ReplaceStrategy())
                .addPatternStrategy("subgraph_final_output", new ReplaceStrategy())
                .addPatternStrategy("streaming_output", new ReplaceStrategy())
                .addPatternStrategy("end_output", new ReplaceStrategy())
                .addPatternStrategy("logs", new AppendStrategy())
                .addPatternStrategy("_graph_execution_id_", new ReplaceStrategy())
                .build();


        // Build the main graph
        StateGraph graph = new StateGraph(keyStrategyFactory)

                // Add nodes
                .addNode("start", node_async(startNode))
                .addNode("parallel1", node_async(parallelNode1))
                .addNode("parallel2", node_async(parallelNode2))
                .addNode("merge", node_async(mergeNode)) // 使用自定义MergeNode并包裹为异步
                .addNode("subgraph", subGraph.subGraph()) // Add subgraph
                .addNode("streaming", node_async(streamingNode)) // Add streaming node
                .addNode("summary", node_async(summaryNode))
                .addNode("end", node_async(endNode))

                // Serial edge: START -> start
                .addEdge(START, "start")

                // Parallel edges: start -> parallel1 and parallel2 (concurrent execution)
                .addEdge("start", "parallel1")
                .addEdge("start", "parallel2")

                // Aggregation edges: both parallel nodes complete -> merge
                .addEdge("parallel1", "merge")
                .addEdge("parallel2", "merge")

                // Serial edge: merge -> subgraph
                .addEdge("merge", "subgraph")

                // Serial edges: subgraph -> streaming -> summary
                .addEdge("subgraph", "streaming")
                .addEdge("streaming", "summary")

                // Serial edge: summary -> end
                .addEdge("summary", "end")

                // Serial edge: end -> END
                .addEdge("end", END);


        // Print graph structure
        GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.PLANTUML, "Observability Demo");

        System.out.println("\n=== Observability Demo Graph ===");
        System.out.println(representation.content());
        System.out.println("================================\n");

        return graph;

    }

    /**
     * 编译状态图，生成可运行的编译后图实例
     * @param observabilityGraph 待编译的状态图对象
     * @param observationCompileConfig 图编译全局配置
     * @return CompiledGraph 可直接执行的编译图实例
     * @throws GraphStateException 图编译失败抛出异常
     */
    @Bean
    public CompiledGraph compiledGraph(StateGraph observabilityGraph, CompileConfig observationCompileConfig)
            throws GraphStateException {

        // 为子图添加 checkpoint saver 配置，确保子图能正确接收输入
        CompileConfig subgraphCompileConfig = CompileConfig.builder(observationCompileConfig)
                .saverConfig(SaverConfig.builder().register(MemorySaver.builder().build()).build())
                .build();

        return observabilityGraph.compile(subgraphCompileConfig);

    }


}
