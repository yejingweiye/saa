
package com.yjw.mcp;

import com.yjw.mcp.service.McpService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@AutoConfiguration
@ComponentScan("com.alibaba.cloud.ai")
public class Nl2sqlMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(Nl2sqlMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider chatBiTools(McpService mcpService) {
        return MethodToolCallbackProvider.builder().toolObjects(mcpService).build();
    }
}
