package com.yjw.bigtool.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.yjw.bigtool.constants.Constant;
import com.yjw.bigtool.service.VectorStoreService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolAgent implements NodeAction {

    private List<Document> documents;

    private ChatClient chatClient;

    private String inputTextKey; // 输入关键词

    private String inputText; // 输入文本

    private VectorStoreService vectorStoreService;

    // 构造方法：从知识库中选择工具
    public ToolAgent(ChatClient chatClient, String inputTextKey, VectorStoreService vectorStoreService) {
        this.chatClient = chatClient;
        this.inputTextKey = inputTextKey;
        this.vectorStoreService = vectorStoreService;
    }

    // 构造方法：从给定的工具列表中选择工具
    public ToolAgent(ChatClient chatClient, String inputTextKey, List<Document> documents) {
        this.documents = documents;
        this.chatClient = chatClient;
        this.inputTextKey = inputTextKey;
    }

    private static final String CLASSIFIER_PROMPT_TEMPLATE = """
			### 工作描述
			你是一个文本关键词提取引擎，能够分析用户传入的问题，并提取句子中的主要关键词。
			### 任务
			你需要从该句子中提取一个或多个关键词，不能遗漏用户描述的主体内容。
			### 约束
			返回多个关键词时，使用空格分隔
			""";


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        if (this.documents == null){
            this.documents = (List<Document>) state.value(Constant.TOOL_LIST).orElseThrow();
        }

        // inputTextkey 传的是input
        if (StringUtils.hasLength(inputTextKey)){
            this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
        }

        ChatResponse response = chatClient.prompt()
                .system(CLASSIFIER_PROMPT_TEMPLATE)
                .user(inputText)
                .call()
                .chatResponse();

        // 查知识库，从这个几个关键中选择最相关的工具
        List<Document> hitTool = vectorStoreService.search(response.getResult().getOutput().getText(), 3);

        // 可能是两个参数
        Map<String,Object> updatedState =  new HashMap<>();
        updatedState.put(Constant.HIT_TOOL,hitTool);
        if(state.value(inputTextKey).isPresent()){
            updatedState.put(inputTextKey, response.getResult().getOutput().getText()); //
        }

        return updatedState ;
    }
}
