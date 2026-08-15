package com.yjw.openai.gpt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletion;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.MediaContent;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.stereotype.Service;

/**
 * 方案 B：用 gpt-image-1 走 chat completions 接口生成图片变体。
 *
 * 把原图作为 image_url 多模态输入，配合提示词让模型直接生成视觉变体。
 * gpt-image-1 不走 images/variations 端点（该端点仅支持 dall-e-2），
 * 因此这里把原图当作多模态输入喂给 chat 模型，生成结果以内联 base64
 * data URL 形式返回在响应的 message content 里。
 */
@Service
public class GptImageVariationService {

    private static final String IMAGE_MODEL = "gpt-image-1";

    private final OpenAiApi openAiApi;

    public GptImageVariationService(OpenAiApi openAiApi) {
        this.openAiApi = openAiApi;
    }

    public List<String> generateVariations(String imageUrl, String prompt, int n) {
        List<MediaContent> content = List.of(
                new MediaContent("text", prompt, null, null, null),
                new MediaContent("image_url", null, new ImageUrl(imageUrl), null, null));

        ChatCompletionMessage userMessage = new ChatCompletionMessage(content, Role.USER);
        ChatCompletionRequest request = new ChatCompletionRequest(List.of(userMessage), false);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(IMAGE_MODEL)
                .N(n)
                .build();
        request = ModelOptionsUtils.merge(options, request, ChatCompletionRequest.class);

        ChatCompletion completion = openAiApi.chatCompletionEntity(request).getBody();
        if (completion == null || completion.choices() == null || completion.choices().isEmpty()) {
            throw new IllegalStateException("gpt-image-1 未返回任何结果");
        }

        List<String> images = new ArrayList<>();
        completion.choices().forEach(choice -> extractImages(choice.message(), images));

        if (images.isEmpty()) {
            throw new IllegalStateException("响应中未解析到图片，请检查 imageUrl 是否可访问且为图片");
        }
        return images;
    }

    /**
     * gpt-image-1 生成的图片以 content parts（image_url data URL 或 b64_json）
     * 返回。rawContent 可能是 List&lt;MediaContent&gt;，也可能是 Jackson 默认反序列化
     * 出的 List&lt;Map&gt;，这里兼容两种形态。
     */
    private void extractImages(ChatCompletionMessage message, List<String> images) {
        Object rawContent = message.rawContent();
        if (rawContent instanceof List<?> parts) {
            for (Object part : parts) {
                if (part instanceof MediaContent media) {
                    if (media.imageUrl() != null) {
                        images.add(media.imageUrl().url());
                    }
                } else if (part instanceof Map<?, ?> map) {
                    if (map.get("image_url") instanceof Map<?, ?> urlMap && urlMap.get("url") != null) {
                        images.add(urlMap.get("url").toString());
                    } else if (map.get("b64_json") != null) {
                        images.add(map.get("b64_json").toString());
                    }
                }
            }
        } else if (rawContent instanceof String text && !text.isBlank()) {
            images.add(text);
        }
    }
}
