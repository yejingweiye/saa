package com.yjw.chatflow.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.yjw.chatflow.config.TodoChatFlowFactory;
import com.yjw.chatflow.config.TodoSubGraphFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/assistant")
public class TodoChatflowController {

    private final CompiledGraph  mainGraph;

    public TodoChatflowController(ChatClient.Builder chatClientBuilder) throws Exception{
        ChatClient chatClient = chatClientBuilder.build();
        // 构建子图
        CompiledGraph subGraph = TodoSubGraphFactory.build(chatClient);

        // 构建主图
        this.mainGraph = TodoChatFlowFactory.build(chatClient, subGraph);
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userInput") String userInput
    ){
        Map<String, Object> input = new HashMap<>();
        input.put("session_id", sessionId);
        input.put("user_input", userInput);

        var stateOpt = mainGraph.invoke(input, RunnableConfig.builder().threadId(sessionId).build());
        OverAllState state = stateOpt.orElseThrow();


        Map<String, Object> result = new HashMap<>();
        result.put("reply", state.value("answer").orElse(""));
        result.put("tasks", state.value("tasks").orElse(List.of()));
        return result;
    }
}
