package com.yjw.modulerag;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class ModuleRAGApplication {

	public static void main(String[] args) {

		SpringApplication.run(ModuleRAGApplication.class, args);
	}

	@Bean
	public ChatMemory chatMemory() {

		return MessageWindowChatMemory.builder().build();
	}

}
