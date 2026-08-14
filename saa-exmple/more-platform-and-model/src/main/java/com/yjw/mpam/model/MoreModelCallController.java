package com.yjw.mpam.model;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/no-model")
public class MoreModelCallController {

    private final Set<String> modelList = Set.of(
            "deepseek-r1",
            "deepseek-v3",
            "qwen-plus",
            "qwen-max"
    );

    private final ChatModel dashScopeChatModel;

    public MoreModelCallController(
            @Qualifier("dashScopeChatModel") ChatModel dashScopeChatModel
    ) {
        this.dashScopeChatModel = dashScopeChatModel;
    }

    @GetMapping("/{model}/{prompt}")
    public String modelChat(
            @PathVariable("model") String model,
            @PathVariable("prompt") String prompt
    ) {
        if (!modelList.contains(model)) {
            return "model not exist";
        }

        System.out.println("===============================================");
        System.out.println("当前输入的模型为：" + model);
        System.out.println("默认模型为：" + DashScopeModel.ChatModel.QWEN_PLUS.getValue());
        System.out.println("===============================================");

        ChatOptions runtimeOptions = ChatOptions.builder().model(model).build();

        Generation gen = dashScopeChatModel.call(
                        new Prompt(prompt, runtimeOptions))
                .getResult();

        return gen.getOutput().getText();
    }
}
