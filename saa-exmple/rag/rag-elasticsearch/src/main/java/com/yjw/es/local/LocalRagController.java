package com.yjw.es.local;

import com.yjw.es.RagService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class LocalRagController {

    private final RagService localRagService;

    public LocalRagController(RagService localRagService) {
        this.localRagService = localRagService;
    }

    @GetMapping("/rag/importDocument")
    public void importDocument() {
        localRagService.importDocuments();
    }

    @GetMapping("/rag")
    public Flux<String> generate(@RequestParam(value = "message",
            defaultValue = "how to get start with spring ai alibaba?") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return localRagService.retrieve(message).map(x -> x.getResult().getOutput().getText());
    }
}
