package com.yjw.modulerag.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扩展查询
 * 用户原始问题：北极有什么动物？
 * 扩展用户原始问题，有利精准获取：
 * 问题1:北极生存的野生动物有哪些
 * 问题2:生活在北极圈的动物种类
 * 问题3:北极地区典型生物
 *
 * 将多条查询全部送入documentRetriever向量库检索
 * documentJoiner合并、去重多路召回结果
 */
@RestController
@RequestMapping("/module-rag")
public class ModuleRAGRewriteController {

    private final ChatClient chatClient;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public ModuleRAGRewriteController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {

        this.chatClient = chatClientBuilder.build();

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .build();

        // 多了这一行
        var queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .targetSearchSystem("vector store") // 告诉大模型：生成扩展查询时，要适配向量数据库（vector store）的检索模式。
                .build();

        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryTransformers(queryTransformer)
                .build();

    }

    @PostMapping("/rag/rewrite")
    public String rag(@RequestBody String prompt) {
        return chatClient.prompt().advisors(retrievalAugmentationAdvisor).user(prompt).call().content();
    }
}
