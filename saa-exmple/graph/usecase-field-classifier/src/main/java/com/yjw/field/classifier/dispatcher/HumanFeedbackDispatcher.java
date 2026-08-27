
package com.yjw.field.classifier.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;


public class HumanFeedbackDispatcher implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        return (String) state.value("human_next_node", "clft");
    }
}
