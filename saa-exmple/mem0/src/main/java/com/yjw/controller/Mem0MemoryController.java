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

package com.alibaba.mem0.example.controller;

import com.alibaba.cloud.ai.memory.mem0.advisor.Mem0ChatMemoryAdvisor;
import com.alibaba.cloud.ai.memory.mem0.core.Mem0ServiceClient;
import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.memory.mem0.advisor.Mem0ChatMemoryAdvisor.USER_ID;


@RestController
@RequestMapping("/advisor/memory/mem0")
public class Mem0MemoryController {
    private static final Logger logger = LoggerFactory.getLogger(Mem0MemoryController.class);

    private final ChatClient chatClient;
    private final VectorStore store;
    private final Mem0ServiceClient mem0ServiceClient;

    public Mem0MemoryController(ChatClient.Builder builder,
                                VectorStore store,
                                Mem0ServiceClient mem0ServiceClient) {
        this.store = store;
        this.mem0ServiceClient = mem0ServiceClient;
        this.chatClient = builder
                .defaultAdvisors(
                        Mem0ChatMemoryAdvisor.builder(store).build()
                )
                .build();
    }

    @GetMapping("/call")
    public String call(@RequestParam(value = "message", defaultValue = "你好，我是万能的喵，我爱玩三角洲行动") String message,
                       @RequestParam(value = "user_id", defaultValue = "miao") String userId
    ) {
        return chatClient.prompt(message)
                .advisors(
                        a -> a.params(Map.of(USER_ID, userId))
                )
                .call().content();
    }

    @GetMapping("/messages")
    public List<Document> messages(
            @RequestParam(value = "query", defaultValue = "我的爱好是什么？") String query,
            @RequestParam(value = "user_id", defaultValue = "miao") String userId) {
        Mem0ServerRequest.SearchRequest searchRequest = Mem0ServerRequest.SearchRequest.mem0Builder()
                .query(query)
                .userId(userId)
                .build();
        return store.similaritySearch(searchRequest);
    }

    @GetMapping("/test")
    public void test() {


        //用户和agent的长期记忆
        mem0ServiceClient.addMemory(
                Mem0ServerRequest.MemoryCreate.builder()
                        .agentId("agent2")
                        .userId("test2")
                        .messages(List.of(
                                new Mem0ServerRequest.Message("user", "I'm travelling to San Francisco"),
                                new Mem0ServerRequest.Message("assistant", "That's great! I'm going to Dubai next month."))
                        )
                        .build());
        logger.info("用户和agent的长期记忆保存成功");
        // 获取用户和agent的长期记忆
        List<Document> documents = store.similaritySearch(Mem0ServerRequest.SearchRequest.mem0Builder()
                .userId("test2")
                .agentId("agent2")
                .build());
        logger.info("agent的长期记忆: {}", documents);


    }
}
