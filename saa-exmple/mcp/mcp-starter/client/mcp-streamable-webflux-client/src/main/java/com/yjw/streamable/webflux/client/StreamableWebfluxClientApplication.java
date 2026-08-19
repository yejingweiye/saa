/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yjw.streamable.webflux.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Scanner;


@SpringBootApplication
public class StreamableWebfluxClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamableWebfluxClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
                                                 ConfigurableApplicationContext context) {

        ToolCallback[] toolCallbacks = tools.getToolCallbacks();
        System.out.println("Available tools:");

        for (ToolCallback toolCallback : toolCallbacks){
            System.out.println(">>> " + toolCallback.getToolDefinition().name());
        }

        return args -> {
            var chatClient = chatClientBuilder
                    .defaultToolCallbacks(tools.getToolCallbacks())
                    .build();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\n>>> QUESTION: ");
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
//                System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
                System.out.print("\n>>> ASSISTANT: ");

                // 流式调用 stream()，不是 call()
                Flux<String> streamFlux = chatClient.prompt(userInput)
                        .stream()
                        .content()
                        // 请求总超时
                        .timeout(Duration.ofSeconds(40))
                        // 捕获网络、MCP调用、模型异常，不崩程序
                        .onErrorResume(ex -> {
                            System.err.println("\n❌ 请求异常：" + ex.getMessage());
                            return Mono.empty();
                        });

                // 命令行阻塞等待流式全部输出完成
                streamFlux
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(System.out::println)
                        .blockLast();

                // 流式结束换行
                System.out.println();
            }

            scanner.close();
            context.close();
        };
    }
}
