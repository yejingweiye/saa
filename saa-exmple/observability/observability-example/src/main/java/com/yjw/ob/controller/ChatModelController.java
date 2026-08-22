package com.yjw.ob.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/observability/chat")
public class ChatModelController {

    private final ChatClient chatClient;

    public ChatModelController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping
    public Flux<String> chat(@RequestParam(defaultValue = "hi") String prompt) {

        return chatClient.prompt(prompt).stream().content();
    }

}
