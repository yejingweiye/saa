package com.yjw.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.dispatcher.HumanFeedbackDispatcher;
import com.yjw.node.ExpanderNode;
import com.yjw.node.HumanFeedbackNode;
import com.yjw.node.TranslateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;


@Configurable
public class WFGraphConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(WFGraphConfiguration.class);

    /**
     * 创建状态图 输入->扩展节点->人类节点->翻译节点->输出
     * 扩展节点：AI 模型流式对问题进行扩展输出
     * 人类节点：通过对用户的反馈，决定是直接结束，还是接着执行翻译节点
     * 翻译节点：将问题翻译为其他英文
     *
     * @param chatClientBuilder
     * @return
     */
    @Bean
    public StateGraph wfGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("query", new ReplaceStrategy())
                .addPatternStrategy("thread_id", new ReplaceStrategy())
                .addPatternStrategy("expander_number", new ReplaceStrategy()) //
                .addPatternStrategy("expander_content", new ReplaceStrategy()) // 。扩展内容
                .addPatternStrategy("feed_back", new ReplaceStrategy())
                .addPatternStrategy("human_next_node", new ReplaceStrategy())
                .addPatternStrategy("translate_language", new ReplaceStrategy())
                .addPatternStrategy("translate_content", new ReplaceStrategy())
                .build();

        // 画图
        StateGraph wfGraph = new StateGraph(keyStrategyFactory)
                .addNode("expander", node_async(new ExpanderNode(chatClientBuilder))) // 扩展节点
                .addNode("human_feedback", node_async(new HumanFeedbackNode())) // 人类反馈节点
                .addNode("translate", node_async(new TranslateNode(chatClientBuilder))) // 翻译节点


                .addEdge(StateGraph.START, "expander")
                .addEdge("expander", "human_feedback")
                .addConditionalEdges(
                        "human_feedback",
                        AsyncEdgeAction.edge_async(new HumanFeedbackDispatcher()),
                        // 分发器返回 "translate" → 进入 translate 节点,返回 StateGraph.END → Graph 流程结束
                        Map.of("translate", "translate", StateGraph.END, StateGraph.END))
                .addEdge("translate", StateGraph.END);

        // 添加打印图
        GraphRepresentation graphRep = wfGraph.getGraph(GraphRepresentation.Type.PLANTUML, "WFGraph");
        logger.info("\n=== expander UML Flow ===");
        logger.info(graphRep.content());
        logger.info("==================================\n");
        return wfGraph;


    }


}
