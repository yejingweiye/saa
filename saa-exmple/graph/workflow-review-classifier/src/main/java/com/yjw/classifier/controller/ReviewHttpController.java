package com.yjw.classifier.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 供工作流 HttpNode 调用的模拟评审接口：负向 /positive 正向。
 */
@RestController
public class ReviewHttpController {

    @GetMapping("/negative")
    public Map<String, Object> negative() {
        return Map.of("result", "negative");
    }

    @GetMapping("/positive")
    public Map<String, Object> positive() {
        return Map.of("result", "positive");
    }
}
