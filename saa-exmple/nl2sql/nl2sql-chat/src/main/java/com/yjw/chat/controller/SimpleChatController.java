package com.yjw.chat.controller;

import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.simple.SimpleNl2SqlService;
import com.alibaba.cloud.ai.service.simple.SimpleVectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * curl -X POST http://127.0.0.1:8065/simpleChat \
 *   -H "Content-Type: application/json" \
 *   -d "查询2025年所有用户的订单总金额"
 * SELECT SUM(total_amount) AS total_amount
 * FROM orders
 * WHERE YEAR(order_date) = 2025;%
 */
@RestController
public class SimpleChatController {

    /**
     * NL2SQL核心服务：负责把自然语言翻译成SQL
     */
    @Autowired
    private SimpleNl2SqlService simpleNl2SqlService;

    /**
     * 向量存储服务：把数据库表结构（表名、字段、注释）向量化存入向量库，给大模型做检索参考
     * 是 Spring‑AI‑Alibaba NL2SQL 封装服务，它底层持有 Spring AI 标准VectorStore向量存储实例
     */
    @Autowired
    private SimpleVectorStoreService simpleVectorStoreService;

    /**
     * 数据库连接配置，yml配置文件注入进来
     */
    @Autowired
    private DbConfig dbConfig;

    // 流程：接收自然语言字符串 → 加载指定 6 张表元数据存入向量库 → 调用 NL2SQL 服务生成 SQL → 返回 SQL 文本
    // 自然语言转SQL接口
    @PostMapping("/simpleChat")
    public String simpleNl2Sql(@RequestBody String input) throws Exception {

        // 构建schema初始化请求对象
        SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
        // 设置数据库连接配置
        schemaInitRequest.setDbConfig(dbConfig);
        // 指定要向量化的表：只把这6张表的表结构元数据加载进向量库，大模型只能参考这几张表
        /*
         * 这是 NL2SQL 的 RAG 检索预处理步骤，不是把表里业务数据存向量库，
         * 而是表元数据（Schema）存入向量库：表名、字段名、字段类型、字段注释、外键关联关系
         * 大模型本身不知道你本地数据库长什么样。
         * 如果你直接把用户问题丢给大模型，它会幻觉：瞎编表名、编字段，写出完全不能执行的 SQL
         */
        schemaInitRequest.setTables(Arrays.asList("categories","order_items","orders","products","users","product_categories"));
        // 调用NL2SQL服务，传入用户自然语言，返回生成SQL字符串
        simpleVectorStoreService.schema(schemaInitRequest);
        return simpleNl2SqlService.nl2sql(input);
    }
}
