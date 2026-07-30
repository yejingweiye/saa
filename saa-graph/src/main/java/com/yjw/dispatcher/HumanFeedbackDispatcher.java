package com.yjw.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HumanFeedbackDispatcher implements EdgeAction {

    private static final Logger logger = LoggerFactory.getLogger(HumanFeedbackDispatcher.class);


    @Override
    public String apply(OverAllState state) throws Exception {
        logger.info("HumanFeedbackDispatcher is running");
        return (String)state.value("human_next_node", StateGraph.END);
    }
}
