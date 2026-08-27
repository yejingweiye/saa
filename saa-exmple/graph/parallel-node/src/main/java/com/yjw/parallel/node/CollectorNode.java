package com.yjw.parallel.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class CollectorNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(CollectorNode.class);

    private static final long TIME_SLEEP = 5000;


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Thread.sleep(TIME_SLEEP);// ?

        String nextStep = END;
        Map<String, Object> updated = new HashMap<>();


        // 扩展和翻译结果有一方不存在则结束，重跑
        if (!areAllExecutionResultsPresent(state)) {
            nextStep = "dispatcher";
        }
        updated.put("collector_next_node", nextStep);
        logger.info("collector node -> {} node", nextStep);
        return updated;
    }

    public boolean areAllExecutionResultsPresent(OverAllState state) {
        return state.value("translate_content").isPresent() && state.value("expander_content").isPresent();
    }

}
