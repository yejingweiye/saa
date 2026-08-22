package com.yjw.ob.controller;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * EmbeddingModelController
 * 作用：提供文本向量化（Embedding）相关接口，演示如何调用 Spring AI 中的 EmbeddingModel。
 * 通过 embedding 接口，能够将一段文本转成向量（Embedding Vector），供后续检索、RAG、相似度匹配等场景使用。
 */
@RestController
@RequestMapping("/observability/embedding")
public class EmbeddingModelController {

    /**
     * Spring 自动注入的 EmbeddingModel
     * 它本质上是一个“文本 -> 向量”的模型适配器，底层可以是 OpenAI、DashScope、Ollama 等不同实现。
     */
    private final EmbeddingModel embeddingModel;

    /**
     * 构造器注入
     * Spring 会自动寻找实现了 EmbeddingModel 的 Bean，并注入到当前控制器中。
     */
    public EmbeddingModelController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * GET /observability/embedding
     * 作用：对单个文本做向量化，返回 embedding 向量长度。
     *
     * 例如：
     *   输入：hello world.
     *   输出：向量维度长度，如 1536
     */
    @GetMapping
    public String embedding() {

        // 调用默认 EmbeddingModel 对“hello world.”做向量化
        var embeddings = embeddingModel.embed("hello world.");

        // 返回向量的长度（维度）
        return "embedding vector size:" + embeddings.length;
    }

    /**
     * GET /observability/embedding/generic
     * 作用：演示更通用的 EmbeddingRequest 写法，
     * 可以显式指定模型、参数、输出格式等。
     *
     * 这里使用 DashScope 的 embedding-v3 模型。
     */
    @GetMapping("/generic")
    public String embeddingGenericOpts() {

        // 构造 EmbeddingRequest，支持批量文本向量化
        // List.of("hello world.") 表示一次只嵌入一个文本
        var embeddings = embeddingModel.call(new EmbeddingRequest(
                List.of("hello world."),
                DashScopeEmbeddingOptions.builder()
                        .model(DashScopeModel.EmbeddingModel.EMBEDDING_V3.getValue())
                        .build())
        ).getResult().getOutput();

        // 返回嵌入结果的向量维度
        return "embedding vector size:" + embeddings.length;
    }
}