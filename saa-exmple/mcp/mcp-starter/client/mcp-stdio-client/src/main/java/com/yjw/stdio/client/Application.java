package com.yjw.stdio.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
                                                 ToolCallbackProvider tools,
                                                 ConfigurableApplicationContext context) {

        // =========【第一部分：这个for循环，Bean初始化阶段执行！】=========
        ToolCallback[] toolCallbacks = tools.getToolCallbacks();
        System.out.println("Available tools:");

        for (ToolCallback toolCallback : toolCallbacks){
            System.out.println(">>> " + toolCallback.getToolDefinition().name());
        }

        // =========【第二部分：lambda里面的代码，容器启动完成后执行】=========
        return args -> {
            ChatClient chatClient = chatClientBuilder
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
