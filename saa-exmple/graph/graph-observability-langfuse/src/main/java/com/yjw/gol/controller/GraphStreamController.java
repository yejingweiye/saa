package com.yjw.gol.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import com.yjw.gol.controller.process.GraphProcess;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 图流式控制器
 *
 * 用于图处理流式运算的REST控制器，提供服务端推送事件（SSE）流式输出接口。
 *
 * 功能特性：
 * - 实时流式输出
 * - 支持SSE协议
 * - 可配置线程管理
 * - 异常处理与日志记录
 *
 */

@RestController
@RequestMapping("/graph/observation")
public class GraphStreamController {

    private static final Logger logger = LoggerFactory.getLogger(GraphStreamController.class);

    @Autowired
    private CompiledGraph compiledGraph;

    /**
     * 每隔 1 秒会收到一个事件，事件内容为：
     * tick 0
     * tick 1
     * tick 2
     * ...
     */
    @GetMapping(value = "/test-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testStream() {
        return Flux.interval(Duration.ofSeconds(1)).map(i -> "tick " + i);
    }


    /**
     * 图流式测试接口
     * @param input 用户输入提示词
     * @param threadId 会话线程ID，用于区分不同会话上下文
     * @return SSE流式Flux数据流
     * @throws GraphRunnerException 图执行运行时异常
     */
    @GetMapping(value = "/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam(value = "prompt",defaultValue = "Hello World")String input,
                                                @RequestParam(value = "thread_id",defaultValue = "observability",required = false) String threadId)
            throws GraphRunnerException {

        logger.info("开始执行流式图任务, 用户输入: {}, 会话线程ID: {}", input, threadId);

        // 构建图运行配置
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();

        // 初始化图状态对象
        Map<String,Object> initialState = new HashMap<>();
        initialState.put("input", input);

        // 创建图流处理器
        GraphProcess graphProcess = new GraphProcess();

        // 获取图执行流式输出
//     AsyncGenerator<NodeOutput> resultStream = compiledGraph.stream(initialState, runnableConfig);
        Flux<NodeOutput> resultStream = compiledGraph.stream(initialState, runnableConfig);

        // 直接返回 Reactor 风格的 Flux，保障链路追踪上下文正常传播
        return graphProcess.processStream(resultStream)
                .doOnCancel(() -> logger.info("客户端断开流式连接"))
                .doOnError(e -> logger.error("流式输出过程发生异常", e))
                .doOnComplete(() -> logger.info("流式输出执行完成"));
    }
}
