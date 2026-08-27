
package com.yjw.field.classifier;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;


@SpringBootApplication
@Slf4j
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner vectorIngestRunner(
            @Value("${rag.source:classpath:rag/rag_friendly_classification.txt}") Resource ragSource,
            EmbeddingModel embeddingModel,
            @Qualifier("classificationVectorStore") VectorStore classificationVectorStore
    ) {
        return args -> {
            logger.info("🔄 正在向量化加载分类分级知识库...");
            var chunks = new TokenTextSplitter().transform(new TextReader(ragSource).read());
            classificationVectorStore.write(chunks);

        };
    }

    /**
     * 分类分级向量存储，用于后续 RAG 检索
     */
    @Bean
    public VectorStore classificationVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore
                .builder(embeddingModel).build();
    }

    /**
     * 多轮对话记忆容器（基于内存）
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().build();
    }

}
