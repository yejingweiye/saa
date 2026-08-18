

package com.yjw.brk.service;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;


public interface RagService {

	void importDocuments();

	Flux<ChatResponse> retrieve(String message);
}
