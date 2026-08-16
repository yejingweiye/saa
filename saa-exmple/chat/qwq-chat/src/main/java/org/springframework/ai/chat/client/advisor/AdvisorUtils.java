
package org.springframework.ai.chat.client.advisor;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.function.Predicate;


/**
 * 你的应用自己的 target/classes 在 classpath 里排在依赖 jar 前面，
 * 所以 JVM 加载的是你写的 AdvisorUtils，jar 里框架那个根本没被加载——这就是"类遮蔽"。
 * 1.JVM 先在target/classes搜到这个类，直接加载你写的版本；
 * 2. 后面框架 jar 里面那个同名的AdvisorUtils，ClassLoader 直接跳过，永远不会被载入 JVM。
 * 3. 框架原本的 AdvisorUtils 完全失效，跑的是你的副本，这就是类遮蔽。
 */
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
