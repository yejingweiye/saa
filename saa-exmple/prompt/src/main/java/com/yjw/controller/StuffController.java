package com.yjw.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;

@RestController
@RequestMapping("/prompt/ai")
public class StuffController {

    private ChatClient chatClient;

    @Autowired
    public StuffController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Value("classpath:/docs/wikipedia-curling.md")
    private Resource docsToStuffResource;

    @Value("classpath:/prompts/qa-prompt.st")
    private Resource qaPromptResource;

    /**
     * 演示使用特定的 prompt 上下文信息以增强大模型的回答。
     */
    @GetMapping("/stuff")
    public Flux<String> completion(
            @RequestParam(value = "message", required = false,
                    defaultValue = "哪些运动员在2022年冬奥会冰壶混合双人赛中获得了金牌") String message,
            @RequestParam(value = "stuffit", defaultValue = "false") boolean stuffit
    ) {
        PromptTemplate promptTemplate = new PromptTemplate(qaPromptResource);

        // 提示词填充
        HashMap<String, Object> map = new HashMap<>();
        map.put("question", message);
        if (stuffit) {
            map.put("context", docsToStuffResource);
        } else {
            map.put("context", "");
        }


        return chatClient.prompt(promptTemplate.create(map)).stream().content();


    }
}
