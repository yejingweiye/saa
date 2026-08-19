

package com.yjw.streamable.webflux.server;

import com.yjw.streamable.webflux.server.service.TimeService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author yingzi
 * @since 2025/10/22
 */
@SpringBootApplication
public class StreamableWebfluxServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamableWebfluxServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider timeTools(TimeService timeService) {
        return MethodToolCallbackProvider.builder().toolObjects(timeService).build();
    }
}
