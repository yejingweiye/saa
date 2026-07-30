package com.yjw.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

public class HumanFeedbackNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        return Map.of();
    }
}
