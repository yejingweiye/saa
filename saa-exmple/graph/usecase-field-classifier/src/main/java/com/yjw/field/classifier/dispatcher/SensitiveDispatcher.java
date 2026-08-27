
package com.yjw.field.classifier.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;


public class SensitiveDispatcher implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        return (String) state.value("is_sensitive").orElse("no");
    }
}
