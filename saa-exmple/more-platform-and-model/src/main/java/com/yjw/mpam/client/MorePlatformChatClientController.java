package com.yjw.mpam.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Objects;

@RestController
@RequestMapping("/more-platform-chat-client")
public class MorePlatformChatClientController {

    private final ChatClient chatClient;

    private final ChatModel ollamaChatModel;

    private final ChatModel openAIChatModel;

    public MorePlatformChatClientController(
            @Qualifier("dashScopeChatModel") ChatModel dashScopeChatModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
            @Qualifier("openAiChatModel") ChatModel openAIChatModel
    ) {

        this.ollamaChatModel = ollamaChatModel;
        this.openAIChatModel = openAIChatModel;

        // 默认使用 DashScopeChatModel 构建
        this.chatClient = ChatClient.builder(dashScopeChatModel).build();
    }

    @GetMapping
    public Flux<String> stream(
            @RequestParam("prompt") String prompt,
            @RequestHeader(value = "platform", required = false) String platform
    ) {
        if (!StringUtils.hasText(platform)) {
            return Flux.just("platform not exist");
        }

        if (Objects.equals("dashscope",platform)){
            System.out.println("命中 dashscope ......");
            return chatClient.prompt(prompt).stream().content();
        }

        if (Objects.equals("ollama",platform)){
            System.out.println("命中 ollama ......");
            return ChatClient.builder(ollamaChatModel).build().prompt(prompt).stream().content();
        }

        if(Objects.equals("openai",platform)){
            System.out.println("命中 openai ......");
            return ChatClient.builder(openAIChatModel).build().prompt(prompt).stream().content();
        }

        return Flux.just("platform not support");

    }
}
