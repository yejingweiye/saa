
package com.yjw.human.node;

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

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("你是信息检索与搜索优化领域的专家。\n你的任务是针对给定查询生成 {number} 个不同版本的查询语句。\n\n每个变体需要从话题的不同角度、不同侧重点进行改写，\n同时保留原始查询的核心意图。目标是扩大搜索范围，提升检索到相关信息的概率。\n\n不要输出解释说明，不要增加其他多余内容。\n各个查询变体使用换行分隔。\n\n原始查询：{query}\n\n查询变体：\n");

    private final ChatClient chatClient;

    private final Integer NUMBER = 3;

    public ExpanderNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        logger.info("expander node is running.");

        String query = state.value("query", "");
        Integer expanderNumber = state.value("expander_number", this.NUMBER);

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt().user((user) -> user.text(DEFAULT_PROMPT_TEMPLATE.getTemplate()).param("number", expanderNumber).param("query", query)).stream().chatResponse();

        return Map.of("expander_content", chatResponseFlux);
    }

}
