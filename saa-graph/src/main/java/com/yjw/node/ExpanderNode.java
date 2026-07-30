package com.yjw.node;

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

    /*
    你是信息检索与搜索优化领域专家。
    你的任务是针对给定查询，生成 {number} 个不同版本的查询变体。
    每个变体需要从不同视角、不同维度覆盖同一主题，同时保留原查询的核心搜索意图。
    目的是扩大检索覆盖范围，提升命中相关信息的概率。
    禁止附带任何解释、额外说明文字。
    仅输出查询变体，每条换行分隔。
    原始查询：{query}
    查询变体列表：
     */
    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate(
            "You are an expert at information retrieval and search optimization.\n" +
                    "Your task is to generate {number} different versions of the given query.\n\n" +
                    "Each variant must cover different perspectives or aspects of the topic,\n" +
                    "while maintaining the core intent of the original query. The goal is to\n" +
                    "expand the search space and improve the chances of finding relevant information.\n\n" +
                    "Do not explain your choices or add any other text.\n" +
                    "Provide the query variants separated by newlines.\n\n" +
                    "Original query: {query}\n\n" +
                    "Query variants:\n");

    private final ChatClient chatClient;

    private final Integer NUMBER = 3;

    public ExpanderNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("Expander mode is running");

        // 优先拿用户预设的参数，没传使用默认的
        String query = state.value("query", "");
        Integer expanderNumber = state.value("expander_number", this.NUMBER);

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt().user((user)-> user
                .text(DEFAULT_PROMPT_TEMPLATE.getTemplate())
                // 提示词传参数
                .param("number", expanderNumber)
                .param("query", query))
                .stream()
                .chatResponse();
        return Map.of("expander_content",chatResponseFlux);
    }
}
