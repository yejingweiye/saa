package com.yjw.mcpconfig;

import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscoveryFactory;
import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yjw.mcpconfig", "com.alibaba.cloud.ai.mcp.router", "com.alibaba.cloud.ai.autoconfigure.mcp.router"})
public class McpMultiConfigApplication {

    private static final Logger log = LoggerFactory.getLogger(McpMultiConfigApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(McpMultiConfigApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(McpServiceDiscovery mcpServiceDiscovery, McpServiceDiscoveryFactory factory) {

        return args -> {

            log.info("=== MCP 多源服务发现演示 ===");
            log.info("已注册的服务发现类型: {}", factory.getRegisteredTypes());
            log.info("注册的服务发现实现数量: {}", factory.size());


            // 测试服务查找
            testServiceDiscovery(mcpServiceDiscovery, "weather-service");
            testServiceDiscovery(mcpServiceDiscovery, "dashscope-chat");
            testServiceDiscovery(mcpServiceDiscovery, "search-service");
            testServiceDiscovery(mcpServiceDiscovery, "non-existent-service");

            log.info("=== 演示完成 ===");

        };

    }

    private void testServiceDiscovery(McpServiceDiscovery discovery, String serviceName) {
        log.info("查找服务: {}", serviceName);

        try {

            McpServerInfo serverInfo = discovery.getService(serviceName);
            if (serverInfo != null) {
                log.info("  ✓ 找到服务: {}", serverInfo.getName());
                log.info("    描述: {}", serverInfo.getDescription());
                log.info("    协议: {}", serverInfo.getProtocol());
                log.info("    版本: {}", serverInfo.getVersion());
                log.info("    端点: {}", serverInfo.getEndpoint());
                log.info("    标签: {}", serverInfo.getTags());
            }
            else{
                log.warn("  ✗ 未找到服务: {}", serviceName);
            }
        } catch (Exception e) {
            log.error("  ✗ 查找服务时发生错误: {}", serviceName, e);
        }
        log.info("");
    }

}
