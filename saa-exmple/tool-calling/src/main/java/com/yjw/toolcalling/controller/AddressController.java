package com.yjw.toolcalling.controller;

import com.alibaba.cloud.ai.toolcalling.baidumap.BaiduMapSearchInfoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yjw.toolcalling.component.AddressInformationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

@RestController
@RequestMapping("/address")
public class AddressController {

    private final ChatClient dashScopeChatClient;
    private final AddressInformationTools addressTools;

    public AddressController(ChatClient chatClient, AddressInformationTools addressTools) {
        this.dashScopeChatClient = chatClient;
        this.addressTools = addressTools;
    }

    /**
     * 不使用工具调用
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "address", defaultValue = "北京") String address) throws JsonProcessingException {

        BaiduMapSearchInfoService.Request query = new BaiduMapSearchInfoService.Request(address);

        return dashScopeChatClient.prompt(new ObjectMapper().writeValueAsString(query))
                .call()
                .content();
    }

    /**
     * 将普通方法封装为工具回调 MethodToolCallback
     */
    @GetMapping("/chat-method-tool-callback")
    public String chatWithBaiduMap(@RequestParam(value = "address", defaultValue = "北京") String address) throws JsonProcessingException {

        Method method = ReflectionUtils.findMethod(AddressInformationTools.class, "getAddressInformation", String.class);

        if (method == null) {
            throw new RuntimeException("找不到目标方法");
        }

        return dashScopeChatClient.prompt(address)
                .toolCallbacks(MethodToolCallback.builder()
                        .toolDefinition(ToolDefinition.builder()
                                .description("调用百度地图API查询地点，获取地址、场所的详细信息，支持地点信息检索查询")
                                .name("getAddressInformation")
                                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                                .build())
                        .toolMethod(method)
                        .toolObject(addressTools)
                        .build())
                .call()
                .content();
    }
}
