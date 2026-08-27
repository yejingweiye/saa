package com.yjw.parallel.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

public class ExpanderNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ExpanderNode.class);

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("你是一名信息检索与搜索优化领域专家。\n你的任务是针对给定查询生成 {number} 个不同版本的查询语句。\n\n每个变体需要从该主题的不同角度或侧重点进行改写，\n同时保留原始查询的核心意图。目标是扩大搜索覆盖范围，提升检索到有效信息的概率。\n\n不要做任何解释，也不要输出额外内容。\n各个查询变体之间使用换行分隔。\n\n原始查询：{query}\n\n查询变体：\n");

    private final ChatClient chatClient;

    private final Integer NUMBER = 3;

    public ExpanderNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    // expand_status：assigned->processing
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        logger.info("expander node is running.");

        String expandStatus = state.value("expand_status", "");
        logger.info("Current expand_status: {}", expandStatus);

        if(!"assigned".equals(expandStatus)){
            return Map.of(); // 未分配结束
        }

        String query = state.value("query", "");
        Integer expanderNumber = state.value("expander_number", this.NUMBER); // 变体数量

        logger.info("Calling LLM for expansion, setting status to processing");

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt().user((user) -> user.text(DEFAULT_PROMPT_TEMPLATE.getTemplate()).param("number", expanderNumber).param("query", query)).stream().chatResponse();

        return Map.of(
                "expander_content", chatResponseFlux,
                "expand_status", "processing"
        );
    }
}
