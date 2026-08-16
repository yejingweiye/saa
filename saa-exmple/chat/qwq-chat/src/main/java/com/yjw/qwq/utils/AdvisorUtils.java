
package com.yjw.qwq.utils;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.function.Predicate;


public final class AdvisorUtils {
    private AdvisorUtils() {
    }

    public static Predicate<ChatClientResponse> onFinishReason() {
        return (chatClientResponse) -> {
            ChatResponse chatResponse = chatClientResponse.chatResponse();
            return chatResponse != null && chatResponse.getResults() != null && chatResponse.getResults().stream().anyMatch((result) -> result != null && result.getMetadata() != null);
        };
    }
}
