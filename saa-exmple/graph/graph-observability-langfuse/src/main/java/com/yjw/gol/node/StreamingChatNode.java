package com.yjw.gol.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 流式聊天节点
 * <p>
 * 支持实时流式输出，以流的形式返回AI响应结果。
 * 该节点通过ChatClient处理输入数据，并生成AI流式响应。
 * <p>
 * 功能特性：
 * - AI实时流式响应输出
 * - 流式请求失败降级兜底机制
 * - 支持自定义输入输出状态键
 * - 完整的执行日志记录
 *
 */

public class StreamingChatNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(StreamingChatNode.class);

    private final String nodeName;

    private final String inputKey;

    private final String outputKey;

    private final ChatClient chatClient;

    private final String prompt;

    public StreamingChatNode(String nodeName, String inputKey, String outputKey, ChatClient chatClient, String prompt) {
        this.nodeName = nodeName;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
        this.chatClient = chatClient;
        this.prompt = prompt;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        logger.info("{} starting streaming processing", nodeName);

        // Get input data
        String inputData = state.value(inputKey).map(Object::toString).orElse("Default input");

        logger.info("{} input data: {}", nodeName, inputData);

        // Build complete prompt
        String fullPrompt = prompt + " Input content: " + inputData;

        // 添加调试信息
        logger.info("{} full prompt length: {} characters", nodeName, fullPrompt.length());
        logger.info("{} using ChatClient: {}", nodeName, chatClient.getClass().getSimpleName());

        try {

            // Create streaming chat response
            // 框架会自动将 Flux<ChatResponse> 转换为 StreamingOutput
//            `Flux<ChatResponse>` 依次发出：`"我"` → `"爱"` → `"中国"`
//            框架转换输出多笔 `StreamingOutput`：
//            1. 第 1 个 StreamingOutput：`message.getText() = "我"`
//            2. 第 2 个 StreamingOutput：`message.getText() = "我爱"`
//            3. 第 3 个 StreamingOutput：`message.getText() = "我爱中国"`
            /**
             *                     ┌─► SSE 实时分片 → 浏览器 (打字机)
             *   StreamingNode ── Flux<ChatResponse> ─┤
             *                       └─► 流结束后聚合完整文本 → state.streaming_output → summary
             *
             *   - 给浏览器：每个 StreamingOutput 分片立刻推出去，所以浏览器逐字打印。
             *   - 给 summary：框架把完整文本写进 streaming_output，summary 拿到完整结果做总结
             *   这两条路是同时进行的。summary 还没跑完时，浏览器就已经把 streaming 的内容打完了。所以不存在"等总结完才到浏览器"
             */
            // 构建AI流式调用，返回ChatResponse对象流
            Flux<ChatResponse> chatResponseFlux = chatClient
                    .prompt()                     // 构建一个新的Prompt请求对象
                    .user(fullPrompt)             // 设置用户侧提示词内容
                    .stream()                     // 开启流式模式，返回Flux流而非Mono一次性结果
                    .chatResponse()               // 流中下发的元素为 ChatResponse 对象（每一块流式分片封装对象）
                    // 当有订阅者订阅这个Flux流时触发回调，只执行一次
                    .doOnSubscribe(sub->logger.info("{}: chatResponseFlux subscribed", nodeName))
                    // 每收到一块AI返回分片，就会触发一次doOnNext，resp为单块ChatResponse
                    .doOnNext(resp -> logger.info("{}: chatResponseFlux emit: {}", nodeName, resp))
                    // 流发生异常时触发，流会终止
                    .doOnError(e -> logger.error("{}: chatResponseFlux error", nodeName, e))
                    // 流正常全部推送完毕、没有报错，触发完成回调
                    .doOnComplete(() -> logger.info("{}: chatResponseFlux complete", nodeName))
                    // 设置整体流超时：从订阅开始，2分钟内流没有完成，直接报超时异常
                    .timeout(java.time.Duration.ofMinutes(2))
                    // 捕获流的异常（包含timeout超时、网络异常、模型报错），降级返回空流，流正常结束不抛出错误
                    .onErrorResume(e -> {
                        logger.error("{}: chatResponseFlux timeout or error, using fallback", nodeName, e);
                        return Flux.empty();
                    });

            logger.info("{} streaming processing setup completed", nodeName);
            // 直接返回 Flux，框架会自动处理流式输出
            return Map.of(outputKey, chatResponseFlux);
        } catch (Exception e) {
            logger.error("{} streaming processing failed: {}", nodeName, e.getMessage(), e);

            // Fallback processing: return regular synchronous response
            String fallbackResult = String.format("[%s] Streaming failed, fallback processing: %s", nodeName,
                    inputData);
            return Map.of(outputKey, fallbackResult);
        }

    }

    public static StreamingChatNode create(String nodeName, String inputKey, String outputKey, ChatClient chatClient,
                                           String prompt) {
        return new StreamingChatNode(nodeName, inputKey, outputKey, chatClient, prompt);
    }

}
