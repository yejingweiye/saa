package com.yjw.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/helloworld")
public class HelloworldController {

    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";
    private final ChatClient dashScopeChatClient;

    public HelloworldController(ChatClient.Builder chatClientBuilder) {
        this.dashScopeChatClient = chatClientBuilder
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .topP(0.7)
                                .build()
                )
                .build();
    }

    /**
     *  http://127.0.0.1:18080/helloworld/stream/chat?query=你好，很高兴认识你，能简单介绍一下自己吗？
     * @param query
     * @param response
     * @return
     */
    //流方式对话
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "query",defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query,
                                   HttpServletResponse response){
        response.setCharacterEncoding("UTF-8");
        return dashScopeChatClient.prompt(query).stream().content();

    }

    /**
     * http://127.0.0.1:18080/helloworld/advisor/chat/123?query=你好，我叫jack，之后的会话中都带上我的名字
     * @param response
     * @param conversationId
     * @param query
     * @return
     */
    // 设置对话ID，使用 ChatMemoryAdvisor 进行上下文记忆
    @GetMapping("/advisor/chat/{conversationId}")
    public Flux<String> advisorChat(
            HttpServletResponse response,
            @PathVariable String conversationId,
            @RequestParam String query
    ){
        response.setCharacterEncoding("UTF-8");
        return dashScopeChatClient
                .prompt(query)
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID,conversationId))
                .stream()
                .content();
    }
}
