package com.yjw.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HumanFeedbackNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(HumanFeedbackNode.class);
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("HumanFeedback node is running");
        HashMap<String,Object> resultMap = new HashMap<>();
        String nextStep = StateGraph.END;

        Map<String, Object> feedBackDataMap = state.data();
        boolean feedBack = (boolean) feedBackDataMap.getOrDefault("feed_back", true);
        if(feedBack){
            nextStep = "translate";
        }

        resultMap.put("human_next_node", nextStep);
        logger.info("human_feedback node -> {} node", nextStep);
        return resultMap;
    }
}
