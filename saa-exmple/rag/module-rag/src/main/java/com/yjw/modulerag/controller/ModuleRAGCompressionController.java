package com.yjw.modulerag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;
/**
 * RewriteQueryTransformer 查询重写转换器
 * 针对单条用户提问做优化改写，增强检索语义，不依赖对话历史
 */

/**
 * 换了这个创建转换器
 * CompressionQueryTransformer
 * CompressionQueryTransformer 查询压缩转换器
 * 面向多轮对话历史，把对话上下文 + 当前用户问题压缩成一条独立简洁查询
 *
 * 场景：带聊天历史的 RAG，用户提问依赖上文。
 * 1.举个对话例子：
 * 用户 1：介绍一下阿尔卑斯山
 * 用户 2：那里有什么野生动物？
 *
 * 原始用户问题：那里有什么野生动物？，单独拿这个句子去向量检索，语义残缺，“那里” 指代不明。
 * CompressionQueryTransformer 传入完整对话历史，让 LLM 生成压缩后的独立查询：
 * 改写输出：阿尔卑斯山有什么野生动物？
 *
 * 2.核心能力
 * 读取对话历史，消解代词（那里、它、这个、上文提到的）
 * 将多轮上下文压缩，输出一条自包含、不需要上下文也能看懂的查询
 * 产出的 query 专门用于向量检索，解决指代歧义问题
 *
 */
@RestController
@RequestMapping("/module-rag")
public class ModuleRAGCompressionController {
    private final ChatClient chatClient;

    private final MessageChatMemoryAdvisor chatMemoryAdvisor;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    public ModuleRAGCompressionController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                                          VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();

        this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();


        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
//                .similarityThreshold(0.50)
                .similarityThreshold(0.10)
                .build();

        var queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .build();

        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryTransformers(queryTransformer)
                .build();

    }

    @PostMapping("/rag/compression/{chatId}")
    public String rag(@RequestBody String prompt, @PathVariable("chatId") String conversationId) {

        return chatClient.prompt()
                .advisors(chatMemoryAdvisor, retrievalAugmentationAdvisor)
                .advisors(advisors -> advisors.param(CONVERSATION_ID,
                        conversationId))
                .user(prompt)
                .call()
                .content();
    }

    }
