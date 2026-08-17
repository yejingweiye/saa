package com.yjw.etl.hebing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagPipelineService ragPipelineService;

    public RagController(RagPipelineService ragPipelineService) {
        this.ragPipelineService = ragPipelineService;
    }

    @GetMapping("/ingest")
    public Map<String, Object> ingest(
            @RequestParam(defaultValue = "classpath:/data/sample.pdf") String path) {
        List<Document> docs = ragPipelineService.ingestPdf(path);
        return Map.of(
                "status", "success",
                "docCount", docs.size()
        );
    }

    @GetMapping("/search")
    public List<Document> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        return ragPipelineService.search(query, topK);
    }
}
