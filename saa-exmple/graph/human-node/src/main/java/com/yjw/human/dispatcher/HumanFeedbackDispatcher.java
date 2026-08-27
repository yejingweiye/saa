
package com.yjw.human.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

// 跳转控制
public class HumanFeedbackDispatcher implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        // 从状态读取 human_next_node，取不到则返回END结束流程
        return (String) state.value("human_next_node", StateGraph.END);
    }
}
