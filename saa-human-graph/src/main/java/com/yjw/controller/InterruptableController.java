package com.yjw.controller;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.yjw.service.InterruptableWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/interruptable")
public class InterruptableController {

    private static final Logger logger = LoggerFactory.getLogger(InterruptableController.class);

    private final InterruptableWorkflowService interruptableWorkflowService;

    public InterruptableController(InterruptableWorkflowService interruptableWorkflowService) {
        this.interruptableWorkflowService = interruptableWorkflowService;
    }

    @GetMapping(value = "/order/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> processOrder(
            @RequestParam String orderId,
            @RequestParam Double amount,
            @RequestParam(defaultValue = "order-thread-1") String threadId)
            throws GraphRunnerException {
        return interruptableWorkflowService.processOrder(orderId, amount, threadId);
    }

    @PostMapping(value = "/order/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> resumeOrderProcess(
            @RequestParam Boolean approved,
            @RequestParam String threadId)
            throws GraphRunnerException {
        return interruptableWorkflowService.resumeOrderProcess(approved, threadId);
    }

    @PostMapping(value = "/operation/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> executeSensitiveOperation(
            @RequestParam String operation,
            @RequestParam(required = false) String params,
            @RequestParam(defaultValue = "operation-thread-1") String threadId)
            throws GraphRunnerException {
        return interruptableWorkflowService.executeSensitiveOperation(operation, params, threadId);
    }

    @PostMapping(value = "/operation/confirm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> confirmSensitiveOperation(
            @RequestParam Boolean confirmed,
            @RequestParam String threadId)
            throws GraphRunnerException {
        return interruptableWorkflowService.confirmSensitiveOperation(confirmed, threadId);
    }
}
