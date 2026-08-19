package com.yjw.sdk.streamable.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.util.Scanner;

@SpringBootApplication
public class SDKClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SDKClientApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public CommandLineRunner predefinedQuestions(
            ChatClient.Builder chatClientBuilder,
            // 直接注入你配置类产出的 AsyncMcpToolCallback
//            AsyncMcpToolCallback mcpToolCallback,
            ToolCallbackProvider mcpToolCallbackProvider,
            ConfigurableApplicationContext context
    ) {
        return args -> {
            ToolCallback[] toolCallbacks;
            try {
                // 把单个callback组装成数组
//                toolCallbacks = new ToolCallback[]{mcpToolCallback};
                toolCallbacks = mcpToolCallbackProvider.getToolCallbacks();
                System.out.println("Available tools:");
                for (ToolCallback t : toolCallbacks) {
                    System.out.println(">>> " + t.getToolDefinition().name());
                }
            } catch (Exception e) {
                System.err.println("获取工具异常:" + e.getMessage());
                e.printStackTrace();
                toolCallbacks = new ToolCallback[0];
            }

            var chatClient = chatClientBuilder
                    .defaultToolCallbacks(toolCallbacks)
                    .build();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\n>>> QUESTION: ");
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
                System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
            }
            scanner.close();
            context.close();
        };
    }
}
