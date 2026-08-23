package com.yjw.gol.node;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 简易子图实现
 * <p>
 * 该子图仅包含串行边，不支持并行处理。子图内部通过多个节点按顺序依次执行处理逻辑。
 * <p>
 * 功能特性：
 * - 纯串行执行流程
 * - 三段式处理流水线
 * - 独立的状态管理
 * - 可配置的处理阶段
 *
 */


public class SimpleSubGraph implements SubGraphNode {

    private final ChatClient chatClient;

    private StateGraph subGraph;

    /**
     * Constructor for SimpleSubGraph
     *
     * @param chatClient the chat client for AI processing
     */
    public SimpleSubGraph(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.subGraph = createSubGraph();
    }

    @Override
    public String id() {
        return "simple_subgraph";
    }

    @Override
    public StateGraph subGraph() {
        return this.subGraph;
    }

    /**
     * 构建子图的内部结构
     *
     * @return 配置完成的子图状态图实例
     */
    private StateGraph createSubGraph() {
        try {
            // Create internal nodes for the subgraph (serial processing)
            ChatNode subNode1 = ChatNode.create("SubGraphNode1", "sub_input", "sub_output1", chatClient,
                    "Please perform the first step processing on the following content:");

            ChatNode subNode2 = ChatNode.create("SubGraphNode2", "sub_output1", "sub_output2", chatClient,
                    "Please perform the second step processing on the following content:");

            ChatNode subNode3 = ChatNode.create("SubGraphNode3", "sub_output2", "subgraph_final_output", chatClient,
                    "Please perform the final processing on the following content:");

            // Build subgraph (pure serial structure)
            return new StateGraph(
                    "Simple SubGraph",
                    () -> {
                Map<String, KeyStrategy> strategies = new HashMap<>();
                strategies.put("sub_input", new ReplaceStrategy());
                strategies.put("sub_output1", new ReplaceStrategy());
                strategies.put("sub_output2", new ReplaceStrategy());
                strategies.put("subgraph_final_output", new ReplaceStrategy());
                strategies.put("logs", new AppendStrategy());
                return strategies;}
            )
                    // Add subgraph nodes
                    .addNode("sub_node1", node_async(subNode1))
                    .addNode("sub_node2", node_async(subNode2))
                    .addNode("sub_node3", node_async(subNode3))

                    // Subgraph edges: pure serial processing
                    .addEdge(START, "sub_node1")
                    .addEdge("sub_node1", "sub_node2")
                    .addEdge("sub_node2", "sub_node3")
                    .addEdge("sub_node3", END);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create subgraph", e);
        }
    }


}
