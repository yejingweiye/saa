package com.yjw.gol.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于ChatClient的聊天节点
 *
 * 标准业务节点，通过ChatClient完成AI交互处理输入数据。
 * 该节点实现同步AI调用处理，并内置请求失败降级机制。
 *
 * 功能特性：
 * - 同步AI模型调用处理
 * - 请求失败降级兜底机制
 * - 可自定义配置输入输出状态键
 * - 执行过程日志记录
 *
 */

public class ChatNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ChatNode.class);

    private final String nodeName;

    private final String inputKey;

    private final String outputKey;

    private final ChatClient chatClient;

    private final String prompt;

    /**
     * Constructor for ChatNode
     * @param nodeName the name of the node
     * @param inputKey the key for input data
     * @param outputKey the key for output data
     * @param chatClient the chat client for AI processing
     * @param prompt the prompt template
     */
    public ChatNode(String nodeName, String inputKey, String outputKey, ChatClient chatClient, String prompt) {
        this.nodeName = nodeName;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
        this.chatClient = chatClient;
        this.prompt = prompt;
    }


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // Get input data Optional<Object>
        String inputData = state.value(inputKey).map(Object::toString).orElse("Default input");

        logger.info("{} is running, inputKey:{}, inputData:{}, state: {}", nodeName, inputKey, inputData,
                JSON.toJSONString(state));

        // Process using ChatClient
        String result;
        try{
            result = chatClient
                    .prompt()
                    .user(prompt + " Input content: " + inputData)
                    .call()
                    .content();

        }catch (Exception e){
            // If ChatClient call fails, use simulated result
            result = String.format("[%s] Simulated processing result: %s", nodeName, inputData);
        }

        logger.info("{} is finished", nodeName);
        // Record execution log
        String logEntry = String.format("[%s] Node execution completed", nodeName);

        Map<String, Object> response = new HashMap<>();
        response.put(outputKey, result);
        response.put("logs", logEntry);

        return response;
    }

    /**
     * Factory method to create ChatNode
     * @param nodeName the name of the node
     * @param inputKey the key for input data
     * @param outputKey the key for output data
     * @param chatClient the chat client for AI processing
     * @param prompt the prompt template
     * @return ChatNode instance
     */
    public static ChatNode create(String nodeName, String inputKey, String outputKey, ChatClient chatClient,
                                  String prompt) {
        return new ChatNode(nodeName, inputKey, outputKey, chatClient, prompt);
    }
}
