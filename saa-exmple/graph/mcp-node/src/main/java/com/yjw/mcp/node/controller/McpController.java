package com.yjw.mcp.node.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/graph/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final CompiledGraph compiledGraph;

    public McpController(@Qualifier("mcpGraph") StateGraph stateGraph) throws GraphStateException {
        this.compiledGraph = stateGraph.compile();
    }

    @GetMapping("/call")
    public Map<String, Object> call(@RequestParam(value = "query", defaultValue = "北京时间现在几点钟", required = false) String query,
                                    @RequestParam(value = "thread_id", defaultValue = "yjw", required = false) String threadId) throws GraphRunnerException {
        //  会话ID
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        //  输入参数
        Map<String, Object> objectMap = new HashMap<>();
        //  设置输入参数
        objectMap.put("query", query);
        //  调用图
        Optional<OverAllState> invoke = this.compiledGraph.invoke(objectMap, runnableConfig);
        //  返回结果
        return invoke.map(OverAllState::data).orElse(new HashMap<>());
    }
}
