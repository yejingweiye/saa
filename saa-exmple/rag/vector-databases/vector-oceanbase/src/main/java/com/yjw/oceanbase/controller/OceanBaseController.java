
package com.yjw.oceanbase.controller;

import com.alibaba.cloud.ai.vectorstore.oceanbase.OceanBaseVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oceanbase")
public class OceanBaseController {

    private static final Logger logger = LoggerFactory.getLogger(OceanBaseController.class);

    @Autowired
    private OceanBaseVectorStore oceanBaseVectorStore;

    @GetMapping("/import")
    public void importData() {
        logger.info("start import data");

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", "12345");
        map.put("year", "2026");
        map.put("name", "zhangsan");
        HashMap<String, Object> map1 = new HashMap<>();
        map.put("id", "12345");
        map.put("year", "2026");
        map.put("name", "lisi");
        List<Document> documents = List.of(
                new Document("世界很大，救赎常在转角处。"),
                new Document("你向前走，面对过去；你转身，迎向未来。", Map.of("year", 2025)),
                new Document("Spring AI 非常强大！Spring AI 非常强大！Spring AI 非常强大！Spring AI 非常强大！Spring AI 非常强大！", map),
                new Document("Spring AI 非常强大！Spring AI 非常强大！Spring AI 非常强大！Spring AI 非常强大！Spring AI 非常强大！", map1)
        );
        oceanBaseVectorStore.add(documents);
    }

    @GetMapping("/search")
    public List<Document> search() {
        logger.info("start search data");
        return oceanBaseVectorStore.similaritySearch(SearchRequest
                .builder()
                .query("Spring")
                .topK(2)
                .build());
    }

    @GetMapping("/filter")
    public List<Document> filter() {
        logger.info("start search data");
        Filter.Expression filter = new Filter.Expression(
                   Filter.ExpressionType.EQ,
                   new Filter.Key("name"),
                   new Filter.Value("lisi")
                );
        return oceanBaseVectorStore.similaritySearch(SearchRequest
                .builder()
                .query("Spring")
                .filterExpression(filter)
                .topK(2)
                .build());
    }

    @GetMapping("/delete")
    public String delete(String id){
        try {
            oceanBaseVectorStore.delete(List.of(id));
            return "delete success";
        } catch (Exception e) {
            logger.error("delete failed", e);
            return "delete failed: " + e.getMessage();
        }
    }
}
