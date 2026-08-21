package com.yjw.chat.controller;

import com.alibaba.cloud.ai.service.analytic.AnalyticNl2SqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 只有配置文件中明确配置 spring.ai.vectorstore.analytic.enabled=true 的时候，
 * 这个被注解标记的配置类 / Bean 才会创建生效；不配置该配置项、或者配置为 false，直接不加载。
 */
@RestController
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticNl2SqlController {

    @Autowired
    private AnalyticNl2SqlService nl2SqlService;

    /**
     * 输入转换为sql
     * 备注：要配置阿里云 AnalyticDB（PostgreSQL 向量引擎）服务
     */
    @PostMapping("/chat")
    public String nl2Sql(@RequestBody String input) throws Exception {
        return nl2SqlService.nl2sql(input);
    }


}
