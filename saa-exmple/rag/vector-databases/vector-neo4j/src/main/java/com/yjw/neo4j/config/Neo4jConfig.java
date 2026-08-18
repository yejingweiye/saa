package com.yjw.neo4j.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jConfig.class);


    @Value("${spring.neo4j.uri}")
    private String uri;
    @Value("${spring.neo4j.authentication.username}")
    private String username;
    @Value("${spring.neo4j.authentication.password}")
    private String password;

    @Value("${spring.ai.vectorstore.neo4j.database-name}")
    private String databaseName;
    @Value("${spring.ai.vectorstore.neo4j.distance-type}")
    private Neo4jVectorStore.Neo4jDistanceType distanceType;
    @Value("${spring.ai.vectorstore.neo4j.index-name}")
    private String indexName;
    @Value("${spring.ai.vectorstore.neo4j.initialize-schema}")
    private boolean initializeSchema;
    @Value("${spring.ai.vectorstore.neo4j.embedding-dimension}")
    private int embeddingDimension;

    @Bean
    public Driver driver(){
        return GraphDatabase.driver(uri,
                AuthTokens.basic(username,password));
    }

    @Bean(name = "neo4jVectorStore")
    public Neo4jVectorStore neo4jVectorStore(Driver driver, EmbeddingModel embeddingModel){
        logger.info("create neo4j vector store");

        return Neo4jVectorStore.builder(driver, embeddingModel)
                .databaseName(databaseName)                // 可选：默认值为 "neo4j"；指定 Neo4j 数据库名称
                .distanceType(distanceType)                // 可选：默认值为 COSINE；指定向量距离计算方式，如余弦距离
                .embeddingDimension(embeddingDimension)    // 可选：默认值为 1536；指定 embedding 向量维度
                .label("Document")                        // 可选：默认值为 "Document"；指定在 Neo4j 中保存文档的标签名
                .embeddingProperty("embedding")            // 可选：默认值为 "embedding"；指定向量属性字段名
                .indexName(indexName)                     // 可选：默认值为 "spring-ai-document-index"；指定向量索引名称
                .initializeSchema(initializeSchema)        // 可选：默认值为 false；是否在启动时自动初始化数据库 schema
                .batchingStrategy(new TokenCountBatchingStrategy()) // 可选：默认值为 TokenCountBatchingStrategy；指定批量写入策略，按 token 数量分批
                .build();

    }

}
