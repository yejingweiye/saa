package com.yjw.ollama.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/client")
public class OllamaChatClientController {

    private static final String DEFAULT_PROMPT="你好，介绍下你自己！请用中文回答。";

    private final ChatClient ollamaiChatClient;


    public OllamaChatClientController(ChatModel chatModel) {

        // 构造时，可以设置 ChatClient 的参数
        // {@link org.springframework.ai.chat.client.ChatClient};
        this.ollamaiChatClient = ChatClient.builder(chatModel)
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        OllamaChatOptions.builder()
                                .topP(0.7)
                                .model("llama3")
                                .build()
                )
                .build();
    }

    /**
     * ChatClient 简单调用
     */
    @GetMapping("/simple/chat")
    public String simpleChat() {

        return ollamaiChatClient.prompt(DEFAULT_PROMPT).call().content();
    }

    /**
     * ChatClient 流式调用
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(HttpServletResponse response) {

        response.setCharacterEncoding("UTF-8");
        return ollamaiChatClient.prompt(DEFAULT_PROMPT).stream().content();
    }





}
