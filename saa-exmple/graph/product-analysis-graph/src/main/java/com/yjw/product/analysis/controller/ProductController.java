package com.yjw.product.analysis.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.yjw.product.analysis.model.Product;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class ProductController {

    private CompiledGraph compiledGraph;

    public ProductController(@Qualifier("productAnalysisGraph") StateGraph productAnalysisGraph)
            throws GraphStateException {

        SaverConfig saverConfig = SaverConfig.builder()
                .register(new MemorySaver())
                .build();

        this.compiledGraph = productAnalysisGraph.compile(CompileConfig.builder().saverConfig(saverConfig).build());

    }

    @PostMapping("/product/enrich")
    public Product enrichProduct(@RequestBody String productDesc) throws GraphRunnerException {
        Map<String, Object> initialState = Map.of("productDesc", productDesc);
        RunnableConfig runnableConfig = RunnableConfig.builder().build();
        Optional<OverAllState> invoke = compiledGraph.invoke(initialState, runnableConfig);
        return (Product) invoke.get().value("finalProduct").orElseThrow();
    }
}
