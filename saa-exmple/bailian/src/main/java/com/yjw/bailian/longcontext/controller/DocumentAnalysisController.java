package com.yjw.bailian.longcontext.controller;



import com.alibaba.cloud.ai.advisor.DashScopeDocumentAnalysisAdvisor;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分析文件中的内容并回答问题
 */
@RestController
public class DocumentAnalysisController {

    private final ChatClient chatClient;

    private static final String DEFAULT_SUMMARY_PROMPT = "总结文本";

    public DocumentAnalysisController(ChatClient.Builder chatClientBuilder,
                                      @Value("${spring.ai.dashscope.api-key}") String apiKey) {
        this.chatClient = chatClientBuilder.defaultAdvisors(
                new DashScopeDocumentAnalysisAdvisor(new SimpleApiKey(apiKey))
        ).build();
    }

    // 上传文档分析，返回内容总结
    @PostMapping(path = "/analyzeByUpload", produces = "text/plain")
    public String analyzeByUpload(@RequestParam("file") MultipartFile file) {
        return chatClient.prompt()
                .advisors(a -> a.param(DashScopeDocumentAnalysisAdvisor.RESOURCE, file.getResource()))
                .user(DEFAULT_SUMMARY_PROMPT)
                .options(DashScopeChatOptions.builder().withModel("qwen-long").build())
                .call()
                .content();
    }

    // 链接文档分析，返回内容总结
    @GetMapping(path = "/analyzeByUrl", produces = "text/plain")
    public String analyzeByUrl(@RequestParam("url") String url) {
        return chatClient.prompt()
                .advisors(a -> a.param(DashScopeDocumentAnalysisAdvisor.RESOURCE, UrlResource.from(url)))
                .user(DEFAULT_SUMMARY_PROMPT)
                .options(DashScopeChatOptions.builder().withModel("qwen-long").build())
                .call()
                .content();
    }



}
