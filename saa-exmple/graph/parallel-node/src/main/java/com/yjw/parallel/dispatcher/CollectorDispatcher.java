
package com.yjw.parallel.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;


public class CollectorDispatcher implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        // 取不到默认end
        return (String) state.value("collector_next_node", StateGraph.END);
    }
} 
