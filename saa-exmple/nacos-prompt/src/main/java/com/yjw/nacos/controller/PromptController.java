package com.yjw.nacos.controller;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplate;
import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/nacos")
public class PromptController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(PromptController.class);

    private final ChatClient client;

    private final ConfigurablePromptTemplateFactory promptTemplateFactory;

    public PromptController(
            ChatModel chatModel,
            ConfigurablePromptTemplateFactory promptTemplateFactory
    ) {

        this.client = ChatClient.builder(chatModel).build();
        this.promptTemplateFactory = promptTemplateFactory;
    }


    @GetMapping("/books")
    public Flux<String> generateJoke(
            @RequestParam(value = "author", required = false, defaultValue = "鲁迅") String authorName,
            HttpServletResponse response
    ) {
        // 防止输出乱码
        response.setCharacterEncoding("UTF-8");

        // 使用 Nacos 存放的提示词模板创建 Prompt
        //  这句话是「按名字取模板，取不到就用代码里的兜底」：
        ConfigurablePromptTemplate template = promptTemplateFactory.create(
                "author",
                "请列出这位{author}最知名的三本书。"
        );
        Prompt prompt = template.create(Map.of("author", authorName));
        logger.info("最终构建的 prompt 为：{}", prompt.getContents());
        return client.prompt(prompt)
                .stream()
                .content();

    }
}
