package com.yjw.gol.controller.process;


import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Map;

/**
 * 图流处理器
 * <p>
 * 负责处理图的流式输出，将NodeOutput节点输出转换为SSE事件。
 * 同时兼容普通节点输出与流式分片输出，完成事件封装格式化。
 * <p>
 * 功能特性：
 * - 流式输出处理
 * - SSE事件报文封装
 * - 异步执行处理
 * - 异常捕获与日志记录
 *
 */
public class GraphProcess {


    private static final Logger logger = LoggerFactory.getLogger(GraphProcess.class);

    /**
     * GraphProcess构造方法
     *
     */
    public GraphProcess() {
    }

    /**
     * 适配Reactor的流式输出处理器。
     * <p>
     * 将 AsyncGenerator<NodeOutput> 转为 Flux<ServerSentEvent<String>>，保证链路追踪上下文不丢失。
     *
     * @param generator 图异步输出生成器，提供各个节点输出数据
     * @return SSE事件的Flux数据流
     */
    public Flux<ServerSentEvent<String>> processStream(AsyncGenerator<NodeOutput> generator) {
        return Flux.create(sink -> processNext(generator, sink));
    }

    public Flux<ServerSentEvent<String>> processStream(Flux<NodeOutput> generator) {
        return Flux.create(sink -> processNext(generator, sink));
    }

    /**
     * 处理Flux版本的图输出流
     * @param generator 节点输出Flux流
     * @param sink SSE输出槽，用于向前端推送事件
     */
    public void processNext(Flux<NodeOutput> generator, FluxSink<ServerSentEvent<String>> sink) {

        // 三个回调参数：onNext接收数据, onError捕获异常, onComplete流正常结束
        generator.subscribe(
                output -> { // 【回调1‑onNext】每收到一条节点输出数据就执行
                    logger.debug("processNext: 收到节点输出, node={}, 数据类型={}, output={}",
                            output != null ? output.node() : null,
                            output != null ? output.getClass().getName() : null,
                            output);

                    String content;
                    if (output instanceof StreamingOutput streamingOutput) { // 流式分片输出，封装json字符串
                        content = JSON.toJSONString(Map.of(
                                "type", "streaming",
                                "node", output.node(),
                                "chunk", streamingOutput.chunk(),
                                "timestamp", System.currentTimeMillis()
                        ));
                    } else {
                        JSONObject nodeOutput = new JSONObject();
                        nodeOutput.put("type", "node_output");
                        nodeOutput.put("node", output.node());
                        nodeOutput.put("data", output.state().data());
                        nodeOutput.put("timestamp", System.currentTimeMillis());
                        content = JSON.toJSONString(nodeOutput);
                    }

                    logger.debug("processNext: 发送SSE事件，节点：{}",
                            output != null ? output.node() : null);

                    sink.next(
                            ServerSentEvent
                                    .builder(content)
                                    .event("node_output")
                                    .id(output.node() + "_" + System.currentTimeMillis())
                                    .build()
                    );


                },
                error -> { // 【回调2‑onError】流发生异常时仅执行一次
                    logger.error("processNext: 数据流处理发生异常", error);
                    // 向前端推送 SSE 事件 {"type":"error","message":"xxx异常信息"}
                    sink.next(ServerSentEvent.builder("{\"type\":\"error\",\"message\":\"" + error.getMessage() + "\"}")
                            .event("error")
                            .build());
                    // 向前端推送SSE 事件 {"type":"completed","message":"Graph processing completed with error"}
                    sink.next(ServerSentEvent.builder("{\"type\":\"completed\",\"message\":\"Graph processing completed with error\"}")
                            .event("completed")
                            .build());
                    // 服务端正式关闭 SSE 长连接
                    sink.complete();

                },
                () -> { // 【回调3‑onComplete】流正常全部跑完、无异常，执行一次
                    logger.debug("processNext: 图流程执行完毕");
                    // {"type":"completed","message":"Graph processing completed"}
                    sink.next(ServerSentEvent.builder("{\"type\":\"completed\",\"message\":\"Graph processing completed\"}")
                            .event("completed")
                            .build());
                    sink.complete();
                }
        );
    }

    /**
     * 处理老版本AsyncGenerator异步生成器，通过拉取模式迭代节点输出
     * @param generator 老版本异步生成器
     * @param sink SSE输出槽
     */
    private void processNext(AsyncGenerator<NodeOutput> generator,
                             reactor.core.publisher.FluxSink<ServerSentEvent<String>> sink) {

        AsyncGenerator.Data<NodeOutput> data = generator.next();

        logger.debug("processNext 被调用: 是否完成={}, 是否错误={}, data={}", data.isDone(), data.isError(), data);

        if (data.isDone()) {
            logger.debug("processNext: 图流程全部执行完成");
            sink.next(ServerSentEvent.builder("{\"type\":\"completed\",\"message\":\"Graph processing completed\"}")
                    .event("completed")
                    .build());
            sink.complete();
            return;
        }

        if (data.isError()) {
            data.getData().whenComplete((v, ex) -> {
                logger.error("processNext: 节点数据执行出现异常", ex);

                sink.next(ServerSentEvent.builder("{\"type\":\"error\",\"message\":\"" + ex.getMessage() + "\"}")
                        .event("error")
                        .build());
                sink.next(ServerSentEvent.builder("{\"type\":\"completed\",\"message\":\"Graph processing completed\"}")
                        .event("completed")
                        .build());
                sink.complete();
            });
            return;
        }

        // 正常节点输出
        data.getData().whenComplete((output, ex) -> {
            if (ex != null) {
                logger.error("processNext: 节点输出处理异常", ex);

                sink.next(ServerSentEvent.builder("{\"type\":\"error\",\"message\":\"" + ex.getMessage() + "\"}")
                        .event("error")
                        .build());
                sink.next(ServerSentEvent.builder("{\"type\":\"completed\",\"message\":\"Graph processing completed\"}")
                        .event("completed")
                        .build());
                sink.complete();
            } else {
                logger.debug("processNext: 节点输出 node={}, 输出类型={}, output={}",
                        output != null ? output.node() : null,
                        output != null ? output.getClass().getName() : null,
                        output);

                String content;
                if (output instanceof StreamingOutput streamingOutput) {
                    content = JSON.toJSONString(Map.of(
                            "type", "streaming",
                            "node", output.node(),
                            "chunk", streamingOutput.chunk(),
                            "timestamp", System.currentTimeMillis()));
                } else {
                    JSONObject nodeOutput = new JSONObject();
                    nodeOutput.put("type", "node_output");
                    nodeOutput.put("node", output.node());
                    nodeOutput.put("data", output.state().data());
                    nodeOutput.put("timestamp", System.currentTimeMillis());
                    content = JSON.toJSONString(nodeOutput);
                }

                logger.debug("processNext: 向客户端发送SSE事件，节点：{}", output != null ? output.node() : null);

                sink.next(ServerSentEvent.builder(content)
                        .event("node_output")
                        .id(output.node() + "_" + System.currentTimeMillis())
                        .build());

                // AsyncGenerator不像Reactor‑Flux，订阅后不会自动持续推送全部数据；
                // 递归推进，继续拉取下一条节点数据，直到data.isDone()为true
                processNext(generator, sink);

            }

        });


    }


}
