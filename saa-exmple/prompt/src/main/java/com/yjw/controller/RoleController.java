package com.yjw.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/example/ai")
public class RoleController {

    private ChatClient chatClient;
    // 加载系统提示词
    @Value("classpath:/prompts/system-message.st")
    private Resource systemPromptResource;

    @Autowired
    public RoleController(ChatClient.Builder chatClientBuilder){
        this.chatClient = chatClientBuilder.build();
    }

    // 提示词角色
    @GetMapping("/roles")
    public Flux<String> roles(
           @RequestParam(value = "message",
                   required = false,
                   defaultValue = "请给我介绍一下海盗黄金时代的三位著名海盗以及他们的事迹。对于每位海盗，请至少写一句话。") String message,
           @RequestParam(value = "name",required = false,defaultValue = "Bob") String name,
           @RequestParam(value = "voice",required = false,defaultValue = "pirate")String voice
    ){
        //  用户输入
        var usermessage = new UserMessage(message);
        // 使用系统模版
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPromptResource);
        // 填充系统提示词的内容，构建完整提示词
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", name, "voice", voice));

        //调用大模型
        return chatClient.prompt(new Prompt(List.of(
                usermessage,
                systemMessage
        )))
                .stream()
                .content();



    }




}
