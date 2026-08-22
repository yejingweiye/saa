package com.yjw.ark.controller;

import com.yjw.ark.controller.helper.FrameExtraHelper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MultiModelController {

    private static final String DEFAULT_PROMPT = "这些是什么？";

    private static final String DEFAULT_VIDEO_PROMPT = "这是一组从视频中提取的图片帧，请描述此视频中的内容。";

    @Autowired
    private ChatModel chatModel;

    private ChatClient openAiChatClient;

    public MultiModelController(ChatModel chatModel) {

        this.chatModel = chatModel;

        // 构造时，可以设置 ChatClient 的参数
        // {@link org.springframework.ai.chat.client.ChatClient};
        this.openAiChatClient = ChatClient.builder(chatModel)
                // 实现 Chat Memory 的 Advisor
                // 在使用 Chat Memory 时，需要指定对话 ID，以便 Spring AI 处理上下文。
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build()
                )
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .topP(0.7)
                                .build()
                )
                .build();
    }

    @GetMapping("/image")
    public String image(
            @RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT) String prompt) throws Exception {

        List<Media> mediaList = List.of(
                new Media(MimeTypeUtils.IMAGE_PNG,
                        new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL().toURI()));

        UserMessage message = UserMessage.builder()
                .text(prompt)
                .media(mediaList)
                .build();


        ChatResponse response = openAiChatClient
                .prompt(new Prompt(message))
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    @GetMapping("/stream/image")
    public String streamImage(@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT) String prompt) {

        UserMessage message = UserMessage.builder()
                .text(prompt)
                .media(new Media(
                        MimeTypeUtils.IMAGE_JPEG,
                        new ClassPathResource("multimodel/dog_and_girl.jpeg")
                ))
                .build();

        List<ChatResponse> response = openAiChatClient
                .prompt(new Prompt(message))
                .stream()
                .chatResponse()
                .collectList()
                .block();

        StringBuilder result = new StringBuilder();
        if (response != null) {
            for (ChatResponse chatResponse : response) {
                String outputContent = chatResponse.getResult().getOutput().getText();
                result.append(outputContent);
            }
        }

        return result.toString();
    }

    @GetMapping("/video")
    public String video(
            @RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_VIDEO_PROMPT)
            String prompt
    ) {

        List<Media> mediaList = FrameExtraHelper.createMediaList(10);

        UserMessage message = UserMessage.builder().text(prompt).media(mediaList).build();

        ChatResponse response = openAiChatClient
                .prompt(new Prompt(message))
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

}
