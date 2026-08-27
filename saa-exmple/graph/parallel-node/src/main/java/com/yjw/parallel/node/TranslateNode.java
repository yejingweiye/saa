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

public class TranslateNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(TranslateNode.class);

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("接收用户查询，将其翻译为 {targetLanguage}。\n如果该查询已经是 {targetLanguage} 语言，则原样返回。\n如果无法识别查询的语言，则原样返回。\n不要添加任何解释以及其他额外文本。\n\n原始查询：{query}\n\n翻译后查询：\n");

    private final ChatClient chatClient;

    private final String TARGET_LANGUAGE = "English";

    public TranslateNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    // translate_status：assigned->processing
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        logger.info("translate node is running.");

        String translateStatus = state.value("translate_status", "");
        logger.info("Current translate_status: {}", translateStatus);

        if (!"assigned".equals(translateStatus)) {
            logger.info("Translate status is not assigned, skipping LLM call");
            return Map.of();
        }

        String query = state.value("query", "");
        String targetLanguage = state.value("translate_language", TARGET_LANGUAGE);


        logger.info("Calling LLM for translation, setting status to processing");

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt().user((user) -> user.text(DEFAULT_PROMPT_TEMPLATE.getTemplate()).param("targetLanguage", targetLanguage).param("query", query)).stream().chatResponse();

        return Map.of(
                "translate_content", chatResponseFlux,
                "translate_status", "processing"
        );
    }
}
