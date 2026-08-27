package com.yjw.parallel.stream.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.yjw.parallel.stream.model.NodeStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;



public class TranslateNode implements NodeAction {

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("Given a user query, translate it to {targetLanguage}.\nIf the query is already in {targetLanguage}, return it unchanged.\nIf you don't know the language of the query, return it unchanged.\nDo not add explanations nor any other text.\n\nOriginal query: {query}\n\nTranslated query:\n");

    private final ChatClient chatClient;

    private final String  TARGET_LANGUAGE= "English";

    private final Map<String, NodeStatus> node2Status;

    public static final String NODE_NAME = "translate";


    public TranslateNode(ChatClient.Builder chatClientBuilder, Map<String, NodeStatus> node2Status) {
        this.chatClient = chatClientBuilder.build();
        this.node2Status = node2Status;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        node2Status.put(NODE_NAME, NodeStatus.RUNNING);

        String query = state.value("query", "");
        String targetLanguage = state.value("translate_language", TARGET_LANGUAGE);

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt().user((user) -> user.text(DEFAULT_PROMPT_TEMPLATE.getTemplate()).param("targetLanguage", targetLanguage).param("query", query)).stream().chatResponse();

        return Map.of("translate_content", chatResponseFlux);
    }
}
