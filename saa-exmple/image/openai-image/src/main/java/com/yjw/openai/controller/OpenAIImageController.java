package com.yjw.openai.controller;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/images")
public class OpenAIImageController {

    private final OpenAiImageModel imageModel;

    @Autowired
    public OpenAIImageController(OpenAiImageModel imageModel) {
        this.imageModel = imageModel;
    }
    // 图像生成
    @GetMapping("/generate")
    public List<String> generateImage(@RequestParam  String prompt,
                                      @RequestParam(defaultValue = "1") int n,
                                      @RequestParam(value = "height", defaultValue = "1024") Integer height,
                                      @RequestParam(value = "width", defaultValue = "1024") Integer width) {
        ImagePrompt imagePrompt = new ImagePrompt(prompt,
                OpenAiImageOptions.builder()
                        .N(n)
                        .width(width)
                        .height(height)
                        .build());

        ImageResponse imageResponse = imageModel.call(imagePrompt);

        return imageResponse.getResults().stream().map(imageResult -> imageResult.getOutput().getUrl()).toList();
    }


    // 基于原图生成多个变体

    /**
     * 方案 A：直接调 OpenAI 原生 variations 端点
     *   用 RestClient 发 multipart 请求，先把 imageUrl 下载成文件再上传。但注意这个端点只支持 dall-e-2（dall-e-3、gpt-image-1 都不支持该端点）。
     *
     *   方案 B：多模态方案（推荐）
     *   用 gpt-image-1 走 chat completions 接口，把原图作为 image_url 多模态输入 + 提示词让它生成变体。这也是 OpenAI 现在的官方推荐做法，
     *   你项目里其他模块已有 DashScope 多模态调用经验，思路一致。
     *    String prompt = """
     *                 参考传入的这张图片，保留原图主体对象、构图位置，生成4张视觉变体。
     *                 分别使用：水彩风格、皮克斯3D、赛博朋克、手绘素描。直接返回生成图片。
     *                 """;
     */
    @GetMapping("/variations")
    public List<String> generateImageVariations(@RequestParam String imageUrl,
                                                @RequestParam(defaultValue = "1") int n,
                                                @RequestParam(value = "height", defaultValue = "1024") Integer height,
                                                @RequestParam(value = "width", defaultValue = "1024") Integer width) {
        ImagePrompt imagePrompt = new ImagePrompt("Create variations of this image",
                OpenAiImageOptions.builder()
                .N(n)
                .width(width)
                .height(height)
                .build());
        // todo 暂时没找到参考图变体相关方法
        return List.of("ok");

    }

    // 图像编辑 这个也没有参考图方法，旧版已被废弃
    @GetMapping("/edit")
    public List<String> generateImageEdit(@RequestParam String imageUrl,
                                          @RequestParam String maskUrl,
                                          @RequestParam(defaultValue = "1") int n,
                                          @RequestParam(value = "height", defaultValue = "1024") Integer height,
                                          @RequestParam(value = "width", defaultValue = "1024") Integer width) {
        ImagePrompt imagePrompt = new ImagePrompt("Create edits for this image",
                OpenAiImageOptions.builder()
                .N(n)
                .width(width)
                .height(height)

                .build());
        // todo 暂时没找到参考图变体相关方法
        return List.of("ok");
    }


}
