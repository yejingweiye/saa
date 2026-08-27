
package com.yjw.field.classifier.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;


@Slf4j
public class SensitiveWordDecNode implements NodeAction {
    public static final String OUTPUT_KEY = "is_sensitive";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Set<String> black = Set.of("暴力", "中国民主党");
        return Map.of(OUTPUT_KEY, black.contains(state.value("field").orElse("")) ? "yes" : "no");
    }
}
