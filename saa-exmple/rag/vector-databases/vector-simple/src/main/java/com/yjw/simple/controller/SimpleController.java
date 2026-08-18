package com.yjw.simple.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/simple")
public class SimpleController {

    private static final Logger logger = LoggerFactory.getLogger(SimpleController.class);

    private final SimpleVectorStore simpleVectorStore;

    private final String SAVE_PATH = System.getProperty("user.dir") + "/saa-exmple/rag" +"/vector-databases/vector-simple/src/main/resources/save.json";

    public SimpleController(EmbeddingModel embeddingModel){
        this.simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
    }

    @GetMapping("/add")
    public void importData(){
        logger.info("开始添加数据");

        HashMap<String,Object> map = new HashMap<>();
        map.put("year",2026);
        map.put("name", "yjw");

        List<Document> documents = List.of(
                new Document("世界很大，救赎隐藏在拐角处"),
                new Document("你向前行，面向过去，同时回望未来。", Map.of("year", 2025)),
                new Document("Spring AI 很棒！！Spring AI 很棒！！Spring AI 很棒！！Spring AI 很棒！！Spring AI 很棒！！", map),
                new Document("1", "测试内容", map)
        );
        this.simpleVectorStore.add(documents);
    }

    @GetMapping("/delete")
    public void delete(){
        logger.info("start delete data");

        this.simpleVectorStore.delete("1"); //     new Document("1", "测试内容", map)

    }

    /**
     * 保存到文件中
     */
    @GetMapping("/save")
    public void save(){
        logger.info("start save data: {}", SAVE_PATH);

        File file = new File(SAVE_PATH);
        if (file.exists()){
            file.delete();
        }

        this.simpleVectorStore.save(file);
    }

    /**
     * 从文件中加载到向量库
     */
    @GetMapping("/load")
    public void load(){
        logger.info("start load data: {}", SAVE_PATH);

        File file = new File(SAVE_PATH);
        this.simpleVectorStore.load(file);
    }

    @GetMapping("/search")
    public List<Document> search(){
        logger.info("start search data");
        return simpleVectorStore.similaritySearch(SearchRequest
                .builder()
                .query("Spring")
                .topK(2)
                .build());
    }

    /**
     * 条件查询
     * @return
     */
    @GetMapping("/search-filter")
    public List<Document> searchFilter() {
        logger.info("start search  filter data");
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.and(
                b.in("year", 2026, 2025),
                b.eq("name", "yjw")
        ).build();

        return simpleVectorStore.similaritySearch(SearchRequest
                .builder()
                .query("Spring")
                .topK(2)
                .filterExpression(expression).build());

    }





}
