
package com.yjw.classifier.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController("/run")
public class GraphRunController {

    private CompiledGraph graph;

    public GraphRunController(@Qualifier("buildGraph") CompiledGraph graph){
        this.graph = graph;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NodeOutput> stream(@RequestBody Map<String, Object> inputs) throws Exception {
        return graph.stream(inputs);
    }


    @PostMapping(value = "/invoke")
    public OverAllState invoke(@RequestBody Map<String, Object> inputs) {
        OverAllState state = graph.invoke(inputs).orElse(null);
        return state;
    }


}
