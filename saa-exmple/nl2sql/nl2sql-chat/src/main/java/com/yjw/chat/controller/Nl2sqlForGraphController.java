package com.yjw.chat.controller;

import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.simple.SimpleVectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.constant.Constant.AGENT_ID;
import static com.alibaba.cloud.ai.constant.Constant.INPUT_KEY;
import static com.alibaba.cloud.ai.constant.Constant.RESULT;

/**
 * StateGraph 图编排里最后节点输出就是完整报告
 */
@RestController
@RequestMapping("nl2sql")
public class Nl2sqlForGraphController {

    private static final Logger logger = LoggerFactory.getLogger(Nl2sqlForGraphController.class);

    private final CompiledGraph compiledGraph;

    // final修饰成员变量：必须在对象实例化阶段就赋值完成
    // 字段@Autowired是对象创建完成之后，Spring 通过反射 set 字段完成依赖注入,不加final
    @Autowired
    private SimpleVectorStoreService simpleVectorStoreService;

    @Autowired
    private DbConfig dbConfig;

    @Autowired
    public Nl2sqlForGraphController(@Qualifier("nl2sqlGraph")StateGraph stateGraph) throws GraphStateException{

        this.compiledGraph = stateGraph.compile();
        this.compiledGraph.setMaxIterations(100);
    }

    // 生成sql+执行返回数据
    @GetMapping("/search")
    public String search(@RequestParam String query) throws Exception {
        // 初始化向量
        SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
        schemaInitRequest.setDbConfig(dbConfig);
        schemaInitRequest.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));


        // 用 schemaForAgent 把 schema 文档存进 agent 999999 的向量库，否则 agentId 过滤召回查不到
        simpleVectorStoreService.schemaForAgent("999999", schemaInitRequest);
        // agentId 必须是非空整数：SqlExecuteNode 用它查 datasource/agent_datasource 表定位执行 SQL 的库。
        // 999999 是 getAgentDbConfig 的兜底 agent
        Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(INPUT_KEY, query, AGENT_ID, "999999"));

        OverAllState overAllState = invoke.get();
        return overAllState.value(RESULT).get().toString();

    }
}
