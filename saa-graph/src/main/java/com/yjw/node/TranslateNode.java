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

public class TranslateNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(TranslateNode.class);

    /*
    根据用户的查询语句，将其翻译为{targetLanguage}语言。
    若原查询本身就是{targetLanguage}语言，则原样返回，不做修改。
    若无法识别原查询的语种，直接原样返回。
    禁止输出任何解释、额外文字。

    原始查询：{query}

    翻译结果：
     */
    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate(
            "Given a user query, translate it to {targetLanguage}.\n" +
                    "If the query is already in {targetLanguage}, return it unchanged.\n" +
                    "If you don't know the language of the query, return it unchanged.\n" +
                    "Do not add explanations nor any other text.\n\n" +
                    "Original query: {query}\n\n" +
                    "Translated query:\n");

    private final ChatClient chatClient;

    private final String  TARGET_LANGUAGE= "English";

    public TranslateNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("Translate node is running");

        String query = state.value("query", "");
        String targetLanguage = state.value("targetLanguage", TARGET_LANGUAGE);

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt().user((user)->user
                .text(DEFAULT_PROMPT_TEMPLATE.getTemplate())
                .param("targetLanguage", targetLanguage)
                .param("query", query))
                .stream()
                .chatResponse();
        return Map.of("translate_content",chatResponseFlux);
    }
}
