package com.yjw.bigtool.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.yjw.bigtool.constants.Constant;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateAgent implements NodeAction {

    private List<Document> documents;

    private ChatClient chatClient;

    private String inputTextKey;

    private String inputText;


    public CalculateAgent(ChatClient chatClient, String inputTextKey) {
        this.chatClient = chatClient;
        this.inputTextKey = inputTextKey;
    }

    private static final String CLASSIFIER_PROMPT_TEMPLATE = """
			### 工作描述
			请使用工具来完成任务
			""";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        if (documents == null) {
            this.documents = (List<Document>) state.value(Constant.HIT_TOOL).orElseThrow();
        }

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        // 编程式手动构建 MethodToolCallback
        // 遍历文档列表，每个Document对应一个工具方法的元数据
        for (Document document : documents) {
            // 从文档元数据中获取方法名、方法参数类型数组，通过反射查找Math类对应的Method对象
            var toolMethod = ReflectionUtils.findMethod(Math.class,
                    document.getMetadata().get(Constant.METHOD_NAME).toString(),
                    (Class<?>[]) document.getMetadata().get(Constant.METHOD_PARAMETER_TYPES));

            // 构建工具定义Builder：工具名称、工具描述、生成方法入参JSON Schema
            DefaultToolDefinition.Builder toolDefinitionBuilder = DefaultToolDefinition.builder()
                    .name(ToolUtils.getToolName(toolMethod))
                    .description(ToolUtils.getToolDescription(toolMethod))
                    .inputSchema(JsonSchemaGenerator.generateForMethodInput(toolMethod));

            // 构建方法工具回调对象，封装工具定义和目标反射方法，用于Spring‑AI ToolCalling调用
            MethodToolCallback build = MethodToolCallback.builder()
                    .toolDefinition(toolDefinitionBuilder.build())
                    .toolMethod(toolMethod)
                    .build();

            // 将构建完成的工具回调加入集合，后续注册给ChatClient供大模型调用
            toolCallbacks.add(build);
        }

        if (StringUtils.hasLength(inputTextKey)) {
            this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
        }


        ChatResponse response = chatClient.prompt()
                .system(CLASSIFIER_PROMPT_TEMPLATE)
                .user(inputText)
                .toolCallbacks(toolCallbacks)  // 大模型 Function Calling 的工具定义：工具 name 必须全局唯一，不支持 Java 的方法重载语义，这是 OpenAI / 通义千问等模型接口的约束，不是 Java 语言约束
                .call()
                .chatResponse();

        Map<String, Object> updatedState = new HashMap<>();
        updatedState.put(Constant.SOLUTION, response.getResult().getOutput().getText());
        return updatedState;
    }
}
