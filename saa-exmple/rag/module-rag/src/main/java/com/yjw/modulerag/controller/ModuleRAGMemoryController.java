package com.yjw.modulerag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 *
 * ModuleRAGBasicController.java
 * 加上了记忆内存
 */
@RestController
@RequestMapping("/module-rag")
public class ModuleRAGMemoryController {

    private final ChatClient chatClient;

    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public ModuleRAGMemoryController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

       var documentRetriever = VectorStoreDocumentRetriever.builder()
               .similarityThreshold(0.50)
               .vectorStore(vectorStore).build();
        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }

    @PostMapping("/rag/memory/{chatId}")
    public String chatWithDocument(@RequestBody String prompt, @PathVariable("chatId") String conversationId) {

        return chatClient.prompt()
                .advisors(chatMemoryAdvisor, retrievalAugmentationAdvisor)
                .advisors(advisors -> advisors.param(CONVERSATION_ID,
                        conversationId))
                .user(prompt)
                .call()
                .content();
    }


}
