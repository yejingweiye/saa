package com.yjw.bigtool.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.bigtool.agent.CalculateAgent;
import com.yjw.bigtool.agent.Tool;
import com.yjw.bigtool.agent.ToolAgent;
import com.yjw.bigtool.constants.Constant;
import com.yjw.bigtool.service.VectorStoreService;
import com.yjw.bigtool.utils.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/bigtool")
public class BigToolController {

    private static final Logger logger = LoggerFactory.getLogger(BigToolController.class);

    private final VectorStoreService vectorStoreService;

    private CompiledGraph compiledGraph;

    private List<Document>  documents = new ArrayList<>();

    public BigToolController(VectorStoreService vectorStoreService, ChatModel chatModel) throws GraphStateException {
        this.vectorStoreService = vectorStoreService;
        this.initializeVectorStore();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        // 定义图
        KeyStrategyFactory  keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy(Constant.INPUT_KEY,new ReplaceStrategy()) // 输入
                .addPatternStrategy(Constant.HIT_TOOL,new ReplaceStrategy()) // 命中工具
                .addPatternStrategy(Constant.SOLUTION,new ReplaceStrategy()) // 解决方案
                .addPatternStrategy(Constant.TOOL_LIST,new ReplaceStrategy()) // 工具列表
                .build();


        // 工具节点
        ToolAgent tools = new ToolAgent(chatClient,Constant.INPUT_KEY,vectorStoreService);

        // 计算代理节点
        CalculateAgent calculateAgent = new CalculateAgent(chatClient, Constant.INPUT_KEY);

        // start->tools->calculate_agent->end
        StateGraph stateGraph = new StateGraph("Consumer Service Workflow Demo", keyStrategyFactory)
                .addNode("tools", AsyncNodeAction.node_async(tools))
                .addNode("calculate_agent", AsyncNodeAction.node_async(calculateAgent))
                .addEdge(StateGraph.START, "tools")
                .addEdge("tools", "calculate_agent")
                .addEdge("calculate_agent", StateGraph.END);

        // 打印图，可以放在MERMAID这展示
        GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.MERMAID,
                "workflow graph");

        System.out.println("\n\n");
        System.out.println(graphRepresentation.content());
        System.out.println("\n\n");

        this.compiledGraph = stateGraph.compile();

    }

    /**
     * 获取Math的所有公开方法，全放知识库了
     */
    private void initializeVectorStore(){

        List<Tool> allTools = new ArrayList<>();

        for(Method method : Math.class.getMethods()){
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                Tool tool = MethodUtils.convertMethodToTool(method);
                if (tool != null) {
                    allTools.add(tool);
                }
            }
        }


        allTools.forEach(tool -> documents.add(new Document(IdUtil.fastSimpleUUID(), tool.getDescription(),
                Map.of(Constant.METHOD_NAME, tool.getName(), Constant.METHOD_PARAMETER_TYPES, tool.getParameterTypes()))));

        vectorStoreService.addDocuments(documents);
    }

    @GetMapping("/search")
    public String search(@RequestParam String query) {
        Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(Constant.INPUT_KEY, query, Constant.TOOL_LIST, documents));
        return invoke.get().value("solution").get().toString();
    }


}
