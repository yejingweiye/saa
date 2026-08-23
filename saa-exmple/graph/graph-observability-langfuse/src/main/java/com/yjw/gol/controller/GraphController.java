package com.yjw.gol.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.observation.metric.SpringAiAlibabaObservationMetricAttributes;
import com.yjw.gol.enums.ObservationMetricAttrs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.management.monitor.MonitorNotification;
import java.util.HashMap;
import java.util.Map;


/**
 * 图控制器
 * <p>
 * 用于执行图处理操作的REST控制器。提供可观测图的同步执行能力。
 * <p>
 * 功能特性：
 * - 图的同步执行
 * - 入参解析处理
 * - 返回结果格式化
 * - 异常捕获处理
 *
 */

@RestController
@RequestMapping("/graph/observation")
public class GraphController {

    @Autowired
    private CompiledGraph compiledGraph;

    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    @GetMapping("/execute")
    public Mono<Map<String, Object>> executeGraph(@RequestParam(value = "prompt", defaultValue = "Hello World") String input) {
        // Capture HTTP span in the main request thread
        final io.opentelemetry.api.trace.Span httpSpan = io.opentelemetry.api.trace.Span.current();
        final String httpSpanId = httpSpan != null ? httpSpan.getSpanContext().getSpanId() : "unknown";
        logger.info("Captured HTTP span ID: {}", httpSpanId);

        /**
         * return Mono.fromCallable(() -> {
         *     // 这里写阻塞/同步业务代码
         * })
         * .subscribeOn(调度器)
         * .doOnSuccess(v -> {
         *     // 成功回调，只做副作用，不修改返回值
         * })
         * .onErrorResume(err -> {
         *     // 发生异常，捕获错误，返回兜底Mono，不让异常往外抛
         * });
         */
        return Mono.fromCallable(() -> {
                    // Create initial state with input
                    Map<String, Object> initialState = new HashMap<>();
                    initialState.put("input", input);

                    try {

                        RunnableConfig runnableConfig = RunnableConfig.builder().build();

                        // Execute graph
                        OverAllState result = compiledGraph.invoke(initialState, runnableConfig).get();

                        // Get final output
                        Object finalOutput = result.value("end_output").orElse("No output");

                        // Return result
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("input", input);
                        response.put("output", finalOutput);
                        response.put("logs", result.value("logs").orElse("No logs"));

                        logger.info("Graph execution completed successfully");
                        return response;
                    } catch (Exception e) {
                        logger.error("Graph execution failed inside callable", e);
                        throw e;
                    }

                })
                .subscribeOn(Schedulers.boundedElastic()) // 指定上游代码跑在 `boundedElastic` 弹性线程池
                .doOnSuccess(response -> { // 成功回调，自定义扩展监控上报属性
                    // Set HTTP observation input/output attributes using captured span
                    try {
                        if (httpSpan != null && response != null) {
                            // Set HTTP-level input
                            httpSpan.setAttribute(ObservationMetricAttrs.LANGFUSE_INPUT.value(), input);
                            httpSpan.setAttribute(ObservationMetricAttrs.GEN_AI_PROMPT.value(), input);

                            // Set HTTP-level output
                            Object output = response.get("output");
                            if (output != null) {
                                String outputText = output.toString();

                                httpSpan.setAttribute(ObservationMetricAttrs.LANGFUSE_OUTPUT.value(), outputText);
                                httpSpan.setAttribute(ObservationMetricAttrs.GEN_AI_COMPLETION.value(), outputText);
                                logger.info("Set HTTP span {} attributes - input: {}, output: {} chars", httpSpanId, input, outputText.length());
                            }


                        }
                    } catch (Exception e) {
                        logger.warn("Failed to set HTTP span attributes: {}", e.getMessage());
                    }
                })
            .onErrorResume(e->{
                logger.error("Graph execution failed: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getMessage());
                return Mono.just(errorResponse);
            });

    }


}
