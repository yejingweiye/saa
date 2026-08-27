package com.yjw.field.classifier.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/sec/graph")
@Slf4j
public class SecGraphController {

    private static final String SSE_UTF8 = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8";

    private final CompiledGraph compiledGraph;

    public SecGraphController(@Qualifier("secGraph") StateGraph stateGraph) throws GraphStateException{
        SaverConfig saverConfig = SaverConfig.builder().register(SaverEnum.MEMORY.getValue(), new MemorySaver()).build();

        this.compiledGraph = stateGraph
                .compile(CompileConfig.builder().saverConfig(saverConfig).interruptBefore("human").build());
    }

    @GetMapping(value = "/chat", produces = SSE_UTF8)
    public Flux<ServerSentEvent<String>> simpleChat(@RequestParam("fieldName") String fieldName,
                                                    @RequestParam(value = "thread_id", defaultValue = "yhong", required = false) String threadId,
                                                    HttpServletResponse response) throws Exception {
        response.setContentType(SSE_UTF8);
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        GraphProcess graphProcess = new GraphProcess(this.compiledGraph);
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<NodeOutput> resultFuture = compiledGraph.fluxStream(Map.of("field", fieldName), runnableConfig);
        graphProcess.processStream(resultFuture, sink);
        return sink.asFlux()
                .doOnCancel(() -> log.info("Client disconnected from stream"))
                .doOnError(e -> log.error("Error occurred during streaming", e));
    }

    @GetMapping(value = "/resume", produces = SSE_UTF8)
    public Flux<ServerSentEvent<String>> resume(@RequestParam(value = "thread_id", defaultValue = "yhong", required = false) String threadId,
                                                @RequestParam(value = "feed_back", defaultValue = "true", required = false) boolean feedBack,
                                                @RequestParam(value = "feedback_reason", defaultValue = "", required = false) String humanReason,
                                                HttpServletResponse response) throws GraphRunnerException {
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        Optional<StateSnapshot> stateSnapshotOpt = this.compiledGraph.stateOf(runnableConfig);
        if (stateSnapshotOpt.isEmpty()) {
            response.setContentType(SSE_UTF8);
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("未找到 thread_id=" + threadId + " 的暂停状态，请先调用 /chat 发起流程")
                    .build());
        }
        response.setContentType(SSE_UTF8);
        StateSnapshot stateSnapshot = stateSnapshotOpt.get();
        OverAllState state = stateSnapshot.state();
        state.withResume();

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("feed_back", feedBack);
        objectMap.put("feedback_reason", humanReason);
        state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "feed_back"));

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        GraphProcess graphProcess = new GraphProcess(this.compiledGraph);
        Flux<NodeOutput> resultFuture = compiledGraph.fluxStreamFromInitialNode(state, runnableConfig);
        graphProcess.processStream(resultFuture, sink);

        return sink.asFlux()
                .doOnCancel(() -> log.info("Client disconnected from stream"))
                .doOnError(e -> log.error("Error occurred during streaming", e));
    }

}
