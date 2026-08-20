package com.yjw.mcp.node.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

// 只是标记 “这个类要做配置文件绑定”，它本身不会把类注册成 Spring Bean
@ConfigurationProperties(prefix = McpNodeProperties.PREFIX)
public class McpNodeProperties {

    public static final String PREFIX = "spring.ai.graph.nodes";

    private Map<String, Set<String>> node2servers;

    public Map<String, Set<String>> getNode2servers() {
        return node2servers;
    }

    public void setNode2servers(Map<String, Set<String>> node2servers) {
        this.node2servers = node2servers;
    }
}
