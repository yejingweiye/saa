
package com.yjw.stream.controller.GraphProcess;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


public class GraphProcess {

    private static final Logger logger = LoggerFactory.getLogger(GraphProcess.class);

    private CompiledGraph compiledGraph;

    public GraphProcess(CompiledGraph compiledGraph) {
        this.compiledGraph = compiledGraph;
    }

    public void processStream(Flux<NodeOutput> nodeOutputFlux, Sinks.Many<ServerSentEvent<ChatMessage>> sink) {
        nodeOutputFlux
                .doOnNext(output -> {
                    logger.info("output = {}", output);
                    String nodeName = output.node();
                    ChatMessage chatMessage = null;
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        String chunk = streamingOutput.chunk();
                        if (chunk != null && !chunk.isEmpty()) {
                            chatMessage = new ChatMessage(nodeName, chunk);
                        }
                    } else {
                        chatMessage = new ChatMessage(nodeName, output.state().data());
                    }
                    sink.tryEmitNext(ServerSentEvent.builder(chatMessage).build());
                })
                .doOnComplete(() -> {
                    // 正常完成
                    sink.tryEmitComplete();
                })
                .doOnError(e -> {
                    logger.error("Error occurred during streaming", e);
                    sink.tryEmitError(e);
                })
                .subscribe();
    }

    public record ChatMessage(@JsonProperty("node_name") String nodeName, @JsonProperty("type") Object data) {
    }
}
