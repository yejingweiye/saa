
package com.yjw.stock;

import com.yjw.stock.service.StockService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StockServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider stockTools(StockService stockService) {
        return MethodToolCallbackProvider.builder().toolObjects(stockService).build();
    }
}
