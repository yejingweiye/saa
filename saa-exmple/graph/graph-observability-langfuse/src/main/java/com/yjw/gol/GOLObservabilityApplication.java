
package com.yjw.gol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 可观测性 Langfuse 示例应用程序
 *
 * 本应用演示包含多种节点类型与边类型的图结构：
 * 开始节点 → 并行节点组 → 并行节点组 → 子图节点 → 流式节点 → 汇总节点 → 结束节点
 *
 * 功能特性：
 * - 并行边：ParallelNode1（并行节点1）与 ParallelNode2（并行节点2）同时执行
 * - 串行边：严格按顺序执行
 * - 子图节点：内部包含串行处理流程
 * - 流式节点：AI 实时流式响应
 * - 每个节点均使用 ChatClient 完成 AI 处理
 *
 */



@SpringBootApplication
public class GOLObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(GOLObservabilityApplication.class, args);
    }
} 
