package com.yjw.toolcalling.controller;

import com.alibaba.cloud.ai.toolcalling.weather.WeatherService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final ChatClient dashScopeChatClient;

    private final WeatherService weatherService;

    public WeatherController(ChatClient chatClient, WeatherService weatherService) {

        this.dashScopeChatClient = chatClient;
        this.weatherService = weatherService;
    }

    /**
     * 不使用工具调用
     */
    @GetMapping("/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "请告诉我北京1天以后的天气") String query) {

        return dashScopeChatClient.prompt(query).call().content();
    }

    /**
     * 使用Function方式实现工具调用 FunctionCallBack
     */
    @GetMapping("/chat-tool-function-name")
    public String chatWithWeatherFunction(@RequestParam(value = "query", defaultValue = "请告诉我北京1天以后的天气") String query) {

        return dashScopeChatClient.prompt(query).toolCallbacks(
                FunctionToolCallback.builder("getWeather", weatherService)
                        .description("调用天气接口，获取城市的天气信息。")
                        .inputType(WeatherService.Request.class)
                        .build()
        ).call().content();
    }

}