

package com.yjw.redis.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/redis")
public class RedisController {

    private static final Logger logger = LoggerFactory.getLogger(RedisController.class);
    private final RedisVectorStore redisVectorStore;

    @Autowired
    public RedisController(@Qualifier("redisVectorStoreCustom") RedisVectorStore redisVectorStore) {
        this.redisVectorStore = redisVectorStore;
    }

    @GetMapping("/import")
    public void importData() {
        logger.info("start import data");

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", "12345");
        map.put("year", "2026");
        map.put("name", "yjw");
        List<Document> documents = List.of(
                new Document("世界很大，救赎常在转角处。"),
                new Document("你向前走，面对过去；你转身，迎向未来。", Map.of("year", 2025)),
                new Document("Spring AI 太强了！！Spring AI 太强了！！Spring AI 太强了！！Spring AI 太强了！！Spring AI 太强了！！", map)
        );
        redisVectorStore.add(documents);
    }

    @GetMapping("/search")
    public List<Document> search() {
        logger.info("start search data");
        return redisVectorStore.similaritySearch(SearchRequest
                .builder()
                .query("Spring")
                .topK(2)
                .build());
    }

    @GetMapping("delete-filter")
    public void deleteFilter() {
        logger.info("start delete data with filter");
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.eq("name", "yjw").build();

        redisVectorStore.delete(expression);
    }
}
