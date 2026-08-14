package com.yjw.mpam.client;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Set;

/**
 * 单纯一个平台的
 */
@RestController
@RequestMapping("/more-model-chat-client")
public class MoreModelChatClientController {

    private final Set<String> modelList = Set.of(
            "deepseek-r1",
            "deepseek-v3",
            "qwen-plus",
            "qwen-max"
    );

    private final ChatClient chatClient;

    public MoreModelChatClientController(
            @Qualifier("dashScopeChatModel") DashScopeChatModel chatModel
    ) {

        // 构建 chatClient
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @GetMapping
    public Flux<String> stream(
            @RequestParam("prompt") String prompt,
            @RequestHeader(value = "models", required = false) String models
    ) {

        if (!models.contains(models)){
            return Flux.just("model not exist");
        }

        return chatClient.prompt(prompt)
                .options(DashScopeChatOptions.builder()
                        .withModel(models) // 这是设置model
                        .build()
                ).stream()
                .content();
    }


}
