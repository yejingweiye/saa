package com.yjw.flight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class FlightApplication {

    private static final Logger logger = LoggerFactory.getLogger(FlightApplication.class);


    public static void main(String[] args) {

        new SpringApplicationBuilder(FlightApplication.class).run(args);
    }

    // 在真实业务场景中，文档的向量化入库通常会独立执行，例如放在CI服务器等任务中处理,现在是启动就写入
    @Bean
    CommandLineRunner ingestTermOfServiceToVectorStore(
            VectorStore vectorStore,
            @Value("classpath:rag/terms-of-service.txt") Resource termsOfServiceDocs
    ) {

        return args -> {
            // 将文档写入向量库
            /*
             * 1、文档读取：TextReader 读取 resources/rag/terms-of-service.txt 文件内容
             * 2、TokenTextSplitter：按照token数量对文本进行切片分割（防止文本过长超出大模型上下文限制）
             * 3、向量化存储：通过 VectorStore.write() 将切分后的文档向量写入向量存储，用于后续RAG检索
             */
           //  defaultChunkSize = 800;      // 每个块最大token数
            // defaultChunkOverlap = 200;   // chunk之间重叠token，保证上下文不被切断
            vectorStore.write(new TokenTextSplitter()
                    .transform(new TextReader(termsOfServiceDocs).read()));

            // 执行相似度检索，做入库校验测试
            vectorStore.similaritySearch("订单退票取消").forEach(doc -> {
                logger.info("检索到相似文档：{}", doc.getText());
            });
        };
    }

    /**
     * 提供基于内存的向量存储（SimpleVectorStore）
     * <p>
     * 依赖 EmbeddingModel（自动注入，Alibaba的嵌入模型）
     * @param embeddingModel
     * @return
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {

        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 存储多轮对话历史（基于内存）
     * 实现上下文感知的连续对话
     * @return
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().build();
    }


    /**
     * 提供可自定义的HTTP客户端（用于调用外部API）
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}