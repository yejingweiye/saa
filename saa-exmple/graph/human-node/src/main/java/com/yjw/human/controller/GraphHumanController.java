package com.yjw.human.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.yjw.human.controller.GraphProcess.GraphProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/graph/human")
public class GraphHumanController {

    private static final Logger logger = LoggerFactory.getLogger(GraphHumanController.class);

    private final CompiledGraph compiledGraph;

    @Autowired
    public GraphHumanController(@Qualifier("humanGraph") StateGraph stateGraph) throws GraphStateException {
        SaverConfig saverConfig = SaverConfig.builder().register(new MemorySaver()).build();

        this.compiledGraph = stateGraph.compile(
                CompileConfig.builder()
                        .saverConfig(saverConfig)
                        .interruptBefore("human_feedback")
                        .build()
        );
    }

    @GetMapping(value = "/expand", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<GraphProcess.ChatMessage>> expand(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？", required = false) String query,
                                                                  @RequestParam(value = "expander_number", defaultValue = "3", required = false) Integer expanderNumber,
                                                                  @RequestParam(value = "thread_id", defaultValue = "yjw", required = false) String threadId) throws GraphRunnerException {
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("query", query);
        objectMap.put("expander_number", expanderNumber);
        Flux<NodeOutput> nodeOutputFlux = compiledGraph.stream(objectMap, runnableConfig);

        // 输出流处理
        Sinks.Many<ServerSentEvent<GraphProcess.ChatMessage>> sink = Sinks.many().unicast().onBackpressureBuffer();
        GraphProcess graphProcess = new GraphProcess(this.compiledGraph);
        graphProcess.processStream(nodeOutputFlux, sink);

        return sink.asFlux()
                .doOnCancel(() -> logger.info("Client disconnected from stream"))
                .doOnError(e -> logger.error("Error occurred during streaming", e));

    }

    @GetMapping(value = "/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<GraphProcess.ChatMessage>> resume(@RequestParam(value = "thread_id", defaultValue = "yjw", required = false) String threadId,
                                                                  @RequestParam(value = "feed_back", defaultValue = "true", required = false) boolean feedBack) throws GraphRunnerException {
        RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
        Optional<StateSnapshot> stateSnapshot = this.compiledGraph.stateOf(config);

        return stateSnapshot.map(state -> {
            try {
                RunnableConfig runnableConfig = this.compiledGraph.updateState(config, Map.of(
                        "feed_back", feedBack
                ), null);
                // 从中断点继续执行工作流
                Flux<NodeOutput> nodeOutputFlux = compiledGraph.stream(null, runnableConfig);

                // 输出流处理
                GraphProcess graphProcess = new GraphProcess(this.compiledGraph);
                Sinks.Many<ServerSentEvent<GraphProcess.ChatMessage>> sink = Sinks.many().unicast().onBackpressureBuffer();
                graphProcess.processStream(nodeOutputFlux, sink);

                return sink.asFlux()
                        .doOnCancel(() -> logger.info("Client disconnected from stream"))
                        .doOnError(e -> logger.error("Error occurred during streaming", e));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).orElseThrow(() -> new GraphRunnerException("State not found for thread ID: " + threadId));
    }
}




