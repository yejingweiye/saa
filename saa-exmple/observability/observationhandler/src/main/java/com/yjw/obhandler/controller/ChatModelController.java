package com.yjw.obhandler.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.yjw.obhandler.observationHandler.CustomerObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/custom/observation/chat")
public class ChatModelController {

    private final DashScopeChatModel dashScopeChatModel;


    public ChatModelController(Environment environment, ObservationRegistry observationRegistry) {

        // 配置handler
        observationRegistry.observationConfig().observationHandler(new CustomerObservationHandler());

        String dashscopeApiKey = environment.getProperty("spring.ai.dashscope.api-key");

        this.dashScopeChatModel = DashScopeChatModel
                .builder()
                .dashScopeApi(DashScopeApi.builder().apiKey(dashscopeApiKey).build())
                .observationRegistry(observationRegistry)
                .build();
    }

    @GetMapping
    public String chat(@RequestParam(name = "message", defaultValue = "hi") String message) {
        return dashScopeChatModel.call(message);
    }
}
