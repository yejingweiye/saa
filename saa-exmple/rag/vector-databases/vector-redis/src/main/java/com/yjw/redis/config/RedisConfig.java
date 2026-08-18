package com.yjw.redis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);


    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.password:}") // 读取密码，没有则为空字符串
    private String password;
    @Value("${spring.ai.vectorstore.redis.prefix}")
    private String prefix;
    @Value("${spring.ai.vectorstore.redis.index}")
    private String indexName;

    @Bean
    public JedisPooled jedisPooled() {
        logger.info("Redis host: {}, port: {}", host, port);
        logger.info("Redis host:{}, port:{}, hasPassword:{}", host, port, !password.isBlank());

        HostAndPort hostAndPort = new HostAndPort(host, port);

        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(5000)
                .socketTimeoutMillis(5000);

        // 设置密码
        if (password != null && !password.isBlank()) {
            clientConfigBuilder.password(password);
        }

        return new JedisPooled(hostAndPort, clientConfigBuilder.build());
    }

    @Bean
    @Qualifier("redisVectorStoreCustom")
    public RedisVectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        logger.info("创建 Redis 向量存储");

        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(indexName) // 可选：索引名称，默认值为 "spring-ai-index"
                .prefix(prefix) // 可选：key 前缀，默认值为 "embedding:"
                .metadataFields( // 可选：定义元数据字段，便于后续按字段过滤
                        RedisVectorStore.MetadataField.tag("name"), // 字符串标签字段，例如用于按 name 过滤
                        RedisVectorStore.MetadataField.numeric("year")) // 数值字段，例如用于按 year 过滤
                .initializeSchema(true) // 可选：是否自动初始化 Redis 索引结构，默认值为 false
                .batchingStrategy(new TokenCountBatchingStrategy()) // 可选：按 token 数量进行批量写入，默认也是 TokenCountBatchingStrategy
                .build();
    }

}
