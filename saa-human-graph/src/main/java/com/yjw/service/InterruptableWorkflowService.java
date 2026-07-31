package com.yjw.service;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class InterruptableWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(InterruptableWorkflowService.class);

    private final CompiledGraph orderApprovalCompiledGraph;
    private final CompiledGraph sensitiveOperationCompiledGraph;

    public InterruptableWorkflowService(
            @Qualifier("orderApprovalGraph") StateGraph orderApprovalGraph,
            @Qualifier("sensitiveOperationGraph") StateGraph sensitiveOperationGraph)
            throws GraphStateException {

        SaverConfig saverConfig = SaverConfig.builder()
                .register(new MemorySaver())
                .build();

        this.orderApprovalCompiledGraph = orderApprovalGraph.compile(
                CompileConfig.builder()
                        .saverConfig(saverConfig)
                        .build()
        );

        this.sensitiveOperationCompiledGraph = sensitiveOperationGraph.compile(
                CompileConfig.builder()
                        .saverConfig(saverConfig)
                        .build()
        );
        logger.info("InterruptableWorkflowService initialized");
    }

    public Flux<ServerSentEvent<String>> processOrder(String orderId, Double amount, String threadId)
            throws GraphRunnerException {
        logger.info("Processing order: orderId={}, amount={}, threadId={}", orderId, amount, threadId);
        Map<String, Object> initialState = new HashMap<>();
        initialState.put("order_id", orderId);
        initialState.put("order_amount", amount);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<NodeOutput> nodeOutputFlux = orderApprovalCompiledGraph.stream(initialState, config);
        processStream(nodeOutputFlux, sink, "Order Processing");

        return sink.asFlux()
                .doOnCancel(() -> logger.info("Client disconnected"))
                .doOnError(e -> logger.error("Error during streaming", e));
    }

    public Flux<ServerSentEvent<String>> resumeOrderProcess(Boolean approved, String threadId)
            throws GraphRunnerException {
        logger.info("Resuming order: approved={}, threadId={}", approved, threadId);
        RunnableConfig baseConfig = RunnableConfig.builder().threadId(threadId).build();
        StateSnapshot snapshot = orderApprovalCompiledGraph.getState(baseConfig);
        if (snapshot == null) {
            logger.error("No state found for threadId: {}", threadId);
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("{\"error\": \"未找到对应的工作流状态，请先发起订单处理\"}")
                    .build());
        }
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("approved", approved);

        Map<String, Object> stateUpdate = new HashMap<>();
        stateUpdate.put("approved", approved);

        RunnableConfig resumeConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
                .addMetadata(RunnableConfig.STATE_UPDATE_METADATA_KEY, stateUpdate)
                .build();

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<NodeOutput> nodeOutputFlux = orderApprovalCompiledGraph.stream(null, resumeConfig);
        processStream(nodeOutputFlux, sink, "Order Resume");

        return sink.asFlux()
                .doOnCancel(() -> logger.info("Client disconnected"))
                .doOnError(e -> logger.error("Error during streaming", e));
    }

    public Flux<ServerSentEvent<String>> executeSensitiveOperation(String operation, String params, String threadId)
            throws GraphRunnerException {
        logger.info("Executing operation: operation={}, params={}, threadId={}", operation, params, threadId);
        Map<String, Object> initialState = new HashMap<>();
        initialState.put("operation", operation);

        Map<String, Object> operationParams = new HashMap<>();
        if (params != null && !params.isEmpty()) {
            String[] pairs = params.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    operationParams.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        initialState.put("operation_params", operationParams);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

        Flux<NodeOutput> nodeOutputFlux = sensitiveOperationCompiledGraph.stream(initialState, config);

        processStream(nodeOutputFlux, sink, "Operation Execution");

        return sink.asFlux()
                .doOnCancel(() -> logger.info("Client disconnected"))
                .doOnError(e -> logger.error("Error during streaming", e));
    }

    public Flux<ServerSentEvent<String>> confirmSensitiveOperation(Boolean confirmed, String threadId)
            throws GraphRunnerException {
        logger.info("Confirming operation: confirmed={}, threadId={}", confirmed, threadId);

        RunnableConfig baseConfig = RunnableConfig.builder().threadId(threadId).build();
        StateSnapshot snapshot = sensitiveOperationCompiledGraph.getState(baseConfig);

        if (snapshot == null) {
            logger.error("No state found for threadId: {}", threadId);
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("{\"error\": \"未找到对应的工作流状态，请先发起操作\"}")
                    .build());
        }

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("confirmed", confirmed);

        RunnableConfig resumeConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
                .build();

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

        Flux<NodeOutput> nodeOutputFlux = sensitiveOperationCompiledGraph.stream(null, resumeConfig);
        processStream(nodeOutputFlux, sink, "Operation Confirmation");

        return sink.asFlux()
                .doOnCancel(() -> logger.info("Client disconnected"))
                .doOnError(e -> logger.error("Error during streaming", e));
    }

    private void processStream(
            Flux<NodeOutput> nodeOutputFlux,
            Sinks.Many<ServerSentEvent<String>> sink,
            String workflowName) {

        nodeOutputFlux.subscribe(
                output -> {
                    try {
                        logger.info("Workflow [{}] output from node: {}", workflowName, output.node());

                        String data;
                        if (output instanceof InterruptionMetadata) {
                            InterruptionMetadata metadata = (InterruptionMetadata) output;
                            data = formatInterruptionMetadata(metadata);
                            logger.info("Workflow [{}] interrupted, waiting for human action", workflowName);
                        } else {
                            data = formatNodeOutput(output);
                        }

                        sink.tryEmitNext(ServerSentEvent.<String>builder().data(data).build());

                    } catch (Exception e) {
                        logger.error("Error processing output", e);
                        sink.tryEmitError(e);
                    }
                },
                error -> {
                    logger.error("Workflow [{}] error", workflowName, error);
                    sink.tryEmitError(error);
                },
                () -> {
                    logger.info("Workflow [{}] completed", workflowName);
                    sink.tryEmitComplete();
                }
        );
    }

    private String formatInterruptionMetadata(InterruptionMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"interruption\",\n");
        sb.append("  \"node\": \"").append(metadata.node()).append("\",\n");

        Optional<Map<String, Object>> metaOpt = metadata.metadata();
        if (metaOpt.isPresent()) {
            Map<String, Object> meta = metaOpt.get();
            sb.append("  \"metadata\": {\n");
            meta.forEach((key, value) -> {
                sb.append("    \"").append(key).append("\": \"").append(value).append("\",\n");
            });
            if (!meta.isEmpty()) {
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }
            sb.append("  },\n");
        }

        sb.append("  \"message\": \"工作流已中断，请处理后继续\"\n");
        sb.append("}");
        return sb.toString();
    }

    private String formatNodeOutput(NodeOutput output) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"node_output\",\n");
        sb.append("  \"node\": \"").append(output.node()).append("\",\n");
        sb.append("  \"state\": ").append(output.state().data()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
