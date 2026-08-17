package com.yjw.modulerag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TranslationQueryTransformer 使用示例
 * 加了这一行： 设置查询转换器 queryTransformer
 * 场景：
 * 你的向量库里面全部是中文文档。
 * 1.用户输入英文提问：What animals live in the North Pole?
 * 2.targetLanguage("chinese") 调用大模型，把问题翻译成中文：北极生活着哪些动物？
 * 3.使用翻译后的中文文本去向量数据库做向量相似度检索。
 */
@RestController
@RequestMapping("/module-rag")
public class ModuleRAGTranslationController {

    private final ChatClient chatClient;

    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public  ModuleRAGTranslationController (ChatClient.Builder chatClientBuilder, VectorStore vectorStore){
        this.chatClient = chatClientBuilder.build();
        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(0.50)
                .vectorStore(vectorStore)
                .build();

        // 设置查询转换器
        var queryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .targetLanguage("chinese") // english
                .build();

        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryTransformers(queryTransformer)
                .build();
    }

    @PostMapping("/rag/translation")
    public String rag(@RequestBody String prompt){
        return chatClient.prompt().advisors(retrievalAugmentationAdvisor).user(prompt).call().content();
    }
}
