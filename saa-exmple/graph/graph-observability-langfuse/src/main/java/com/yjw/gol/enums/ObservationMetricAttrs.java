package com.yjw.gol.enums;


/**
 * 扩展观测属性，包含原SpringAiAlibaba全部属性 + Langfuse OTel GenAI扩展字段
 */
public enum ObservationMetricAttrs {

    // ========== 来自第三方 SpringAiAlibabaObservationMetricAttributes 原有字段 ==========
    GRAPH_NAME("spring.ai.alibaba.graph.name"),
    GRAPH_SUCCESS("spring.ai.alibaba.graph.success"),
    GRAPH_NODE_NAME("spring.ai.alibaba.graph.node.name"),
    GRAPH_NODE_SUCCESS("spring.ai.alibaba.graph.node.success"),
    GRAPH_EDGE_NAME("spring.ai.alibaba.graph.edge.name"),
    GRAPH_EDGE_SUCCESS("spring.ai.alibaba.graph.edge.success"),

    // ========== 新增 Langfuse / OTel GenAI 属性 ==========
    LANGFUSE_INPUT("langfuse.input"),
    LANGFUSE_OUTPUT("langfuse.output"),
    GEN_AI_PROMPT("gen_ai.prompt"),
    GEN_AI_COMPLETION("gen_ai.completion");


    private final String value;

    ObservationMetricAttrs(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

