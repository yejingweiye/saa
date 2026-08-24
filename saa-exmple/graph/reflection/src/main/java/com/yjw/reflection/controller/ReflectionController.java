package com.yjw.reflection.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reflection")
public class ReflectionController {
    private static final Logger logger = LoggerFactory.getLogger(ReflectionController.class);

    private CompiledGraph compiledGraph;

    @GetMapping("/chat")
    public String simpleChat(String query) {
        return compiledGraph
                // invoke：执行图，传入输入参数Map
                // Map.of("messages", List.of(new UserMessage(query)))：入参，给图的上下文传入messages列表，里面封装用户提问消息
                .invoke(Map.of("messages", List.of(new UserMessage(query))))
                // get()：阻塞获取Graph执行完成后的结果（同步调用，等待整个工作流跑完）
                .get()
                // 从返回结果对象中拿key为"messages"的值，强转成List<Message>；messages保存整个会话全部消息（用户、助手等）
                .<List<Message>>value("messages")
                // 如果拿不到messages直接抛异常，不允许为空
                .orElseThrow()
                // 转成stream流处理消息列表
                .stream()
                // 过滤：只保留类型为ASSISTANT的消息，排除UserMessage、SystemMessage等
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT)
                // reduce((first, second) -> second)：取流里面最后一个元素
                // 遍历全部助手消息，不断把second赋值给累计值，循环结束后累计值就是最后一条助手消息
                .reduce((first, second) -> second)
                // Optional<Message> 如果有值，调用getText()拿到消息文本
                .map(Message::getText)
                // 如果没有任何助手消息，抛出异常，不返回null
                .orElseThrow();
    }
}
