package com.yjw.openai.gpt;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gpt/images")
public class GptImageVariationController {

    private final GptImageVariationService variationService;

    public GptImageVariationController(GptImageVariationService variationService) {
        this.variationService = variationService;
    }

    @GetMapping("/variations")
    public List<String> generateVariations(@RequestParam String imageUrl,
                                           @RequestParam(defaultValue = "4") int n,
                                           @RequestParam(required = false) String prompt) {
        if (prompt == null || prompt.isBlank()) {
            prompt = """
                    参考传入的这张图片，保留原图主体对象、构图位置，生成 %d 张视觉变体。
                    分别使用：水彩风格、皮克斯3D、赛博朋克、手绘素描。直接返回生成图片。
                    """.formatted(n);
        }
        return variationService.generateVariations(imageUrl, prompt, n);
    }
}
