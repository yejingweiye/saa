
package com.yjw.chat;

import com.alibaba.cloud.ai.vectorstore.analyticdb.AnalyticDbVectorStore;
import com.alibaba.cloud.ai.vectorstore.analyticdb.AnalyticDbVectorStoreProperties;
import com.aliyun.gpdb20160503.Client;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// com.alibaba.cloud.ai
@SpringBootApplication(scanBasePackages = {"com.yjw.chat","com.alibaba.cloud.ai"})
@AutoConfiguration
@ConditionalOnClass({ EmbeddingModel.class, Client.class, AnalyticDbVectorStore.class })
@EnableConfigurationProperties({ AnalyticDbVectorStoreProperties.class })
public class Nl2sqlChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(Nl2sqlChatApplication.class, args);
    }

}
