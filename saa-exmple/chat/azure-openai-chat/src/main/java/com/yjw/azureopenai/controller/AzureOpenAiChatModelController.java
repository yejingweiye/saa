
package com.yjw.azureopenai.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("azure/openai")
public class AzureOpenAiChatModelController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己吧。";

    private final ChatModel azureOpenAiChatModel;

    private final ChatClient azureOpenAiChatClient;

    public AzureOpenAiChatModelController(ChatModel chatModel) {
        this.azureOpenAiChatModel = chatModel;
        this.azureOpenAiChatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 最简单的使用方式，没有任何 LLMs 参数注入。
     *
     * @return String types.
     */
    @GetMapping("chat-model/simple/chat")
    public String simpleChat() {

        return azureOpenAiChatModel.call(new Prompt(DEFAULT_PROMPT)).getResult().getOutput().getText();
    }

    /**
     * Stream 流式调用。可以使大模型的输出信息实现打字机效果。
     *
     * @return Flux<String> types.
     */
    @GetMapping("chat-model/stream/chat")
    public Flux<String> streamChat(HttpServletResponse response) {

        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");

        Flux<ChatResponse> chatResponseFlux = azureOpenAiChatModel.stream(new Prompt(DEFAULT_PROMPT));
        return chatResponseFlux.map(resp -> resp.getResult().getOutput().getText());
    }

    @GetMapping("chat-client/chat")
    public String clientChat() {
        return azureOpenAiChatClient.prompt(new Prompt(DEFAULT_PROMPT)).call().chatResponse().getResult().getOutput().getText();
    }

    @GetMapping("client/stream/chat")
    public Flux<String> clientStreamChat(HttpServletResponse response) {

        response.setCharacterEncoding("UTF-8");
        return azureOpenAiChatClient.prompt(DEFAULT_PROMPT).stream().content();
    }


}
