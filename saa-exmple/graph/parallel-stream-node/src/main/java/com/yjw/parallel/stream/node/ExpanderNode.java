package com.yjw.parallel.stream.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.yjw.parallel.stream.model.NodeStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;



public class ExpanderNode implements NodeAction {

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("You are an expert at information retrieval and search optimization.\nYour task is to generate {number} different versions of the given query.\n\nEach variant must cover different perspectives or aspects of the topic,\nwhile maintaining the core intent of the original query. The goal is to\nexpand the search space and improve the chances of finding relevant information.\n\nDo not explain your choices or add any other text.\nProvide the query variants separated by newlines.\n\nOriginal query: {query}\n\nQuery variants:\n");

    private final ChatClient chatClient;

    private final Integer NUMBER = 3;

    private final Map<String, NodeStatus> node2Status;

    public static final String NODE_NAME = "expander";

    public ExpanderNode(ChatClient.Builder chatClientBuilder, Map<String, NodeStatus> node2Status) {
        this.chatClient = chatClientBuilder.build();
        this.node2Status = node2Status;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        node2Status.put(NODE_NAME, NodeStatus.RUNNING);

        String query = state.value("query", "");
        Integer expanderNumber = state.value("expander_number", this.NUMBER);

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt().user((user) -> user.text(DEFAULT_PROMPT_TEMPLATE.getTemplate()).param("number", expanderNumber).param("query", query)).stream().chatResponse();

        return Map.of("expander_content", chatResponseFlux);
    }
}
