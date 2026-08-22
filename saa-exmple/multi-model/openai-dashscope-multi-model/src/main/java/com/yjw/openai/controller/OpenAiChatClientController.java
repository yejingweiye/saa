package com.yjw.openai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("openai/client")
public class OpenAiChatClientController {

    private final ChatClient chatClient;

    public OpenAiChatClientController(@Qualifier("openAiChatModel") ChatModel chatModel) {
        chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.7).build())
                .build();

    }

    /**
     * 图片分析接口 - 通过 URL
     * 使用 OpenAI 多模态模型分析图片内容
     */
    @GetMapping("/image/analyze/url")
    public String analyzeImageByUrl(@RequestParam(defaultValue = "请分析这张图片的内容") String prompt,
                                    @RequestParam String imageUrl) {
        try {

            // 创建包含图片的用户消息
            String mineType = determineMimeTypeFromUrl(imageUrl);

            Media media = Media.builder()
                    .data(new URI(imageUrl))
                    .mimeType(MimeTypeUtils.parseMimeType(mineType))
                    .build();

            UserMessage message = UserMessage.builder()
                    .text(prompt)
                    .media(media)
                    .build();

            // 创建提示词，使用 OpenAI 多模态模型
            Prompt chatPrompt = new Prompt(message,
                    OpenAiChatOptions.builder()
                            .model("qwen-vl-max")  // 使用 OpenAI 视觉模型
                            .temperature(0.7)
                            .maxTokens(1000)
                            .build());

            // 调用模型进行图片分析
            return chatClient.prompt(chatPrompt).call().content();

        } catch (Exception e) {
            return "图片分析失败: " + e.getMessage();
        }
    }

    /**
     * 图片分析接口 - 通过文件上传
     */
    @PostMapping("/image/analyze/upload")
    public String analyzeImageByUpload(@RequestParam(defaultValue = "请分析这张图片的内容") String prompt,
                                       @RequestParam("file") MultipartFile file) {
        try {
            if(!file.getContentType().startsWith("image/")){
                return "请上传图片文件";
            }

            // 创建包含图片的用户消息
            Media media = new Media(MimeTypeUtils.parseMimeType(file.getContentType()),file.getResource());
            UserMessage message = UserMessage.builder()
                    .text(prompt)
                    .media(media)
                    .build();

            // 创建提示词，启用多模态模型
            // 创建提示词，使用 OpenAI 多模态模型
            Prompt chatPrompt = new Prompt(message,
                    OpenAiChatOptions.builder()
                            .model("qwen-vl-max")  // 使用 OpenAI 视觉模型
                            .temperature(0.7)
                            .maxTokens(1000)
                            .build());

            // 调用模型进行图片分析
            return chatClient.prompt(chatPrompt).call().content();

        } catch (Exception e) {
            return "图片分析失败: " + e.getMessage();

        }
    }


    /**
     * 根据URL确定MIME类型
     */
    public String determineMimeTypeFromUrl(String imageUrl) {
        String lowerUrl = imageUrl.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        } else {
            // 默认使用JPEG
            return "image/jpeg";
        }
    }
}
