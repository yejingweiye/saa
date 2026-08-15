

package com.yjw.deepseek.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/model")
public class DeepSeekChatModelController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己吧。";

    private final ChatModel DeepSeekChatModel;

    public DeepSeekChatModelController(ChatModel chatModel) {
        this.DeepSeekChatModel = chatModel;
    }

    /**
     * 最简单的使用方式，没有任何 LLMs 参数注入。
     *
     * @return String types.
     */
    @GetMapping("/simple/chat")
    public String simpleChat () {
        return DeepSeekChatModel.call(new Prompt(DEFAULT_PROMPT)).getResult().getOutput().getText();
    }

    /**
     * Stream 流式调用。可以使大模型的输出信息实现打字机效果。
     *
     * @return Flux<String> types.
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat (HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Flux<ChatResponse> stream = DeepSeekChatModel.stream(new Prompt(DEFAULT_PROMPT));
        return stream.map(resp -> resp.getResult().getOutput().getText());
    }

}
