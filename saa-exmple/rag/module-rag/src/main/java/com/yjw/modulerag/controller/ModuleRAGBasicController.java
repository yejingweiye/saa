package com.yjw.modulerag.controller;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/module-rag")
public class ModuleRAGBasicController {


    private final ChatClient chatClient;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public ModuleRAGBasicController(ChatClient.Builder chatBuilder, VectorStore vectorStore) {
        this.chatClient = chatBuilder.build();
        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(0.50)
                .vectorStore(vectorStore)
                .build();

        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();

    }

    @GetMapping("/rag/basic")
    public String chatWithDocument(@RequestParam("prompt") String prompt) {

        return chatClient.prompt().advisors(retrievalAugmentationAdvisor).user(prompt).call().content();
    }


}
