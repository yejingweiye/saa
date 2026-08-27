
package com.yjw.human.node;

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
    public Map<String, Object> apply(OverAllState state) {
        logger.info("human_feedback node is running.");
        HashMap<String, Object> resultMap = new HashMap<>();
        String nextStep = StateGraph.END; // 默认是结束

        Map<String, Object> feedBackData = state.data();
        boolean feedback = (boolean) feedBackData.getOrDefault("feed_back", true);
        if (feedback) {
            nextStep = "translate";
        }

        resultMap.put("human_next_node", nextStep);
        logger.info("human_feedback node -> {} node", nextStep);
        return resultMap;
    }
}
