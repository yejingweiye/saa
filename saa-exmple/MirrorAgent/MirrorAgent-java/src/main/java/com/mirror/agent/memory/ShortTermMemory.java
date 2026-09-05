package com.mirror.agent.memory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短期记忆：对话上下文滑动窗口（与 Go 版本一致，默认 20 条消息）
 */
@Component
public class ShortTermMemory {

    /**
     * 滑动窗口大小：20 条消息，与 Go 版本一致
     */
    private static final int DEFAULT_MAX_LEN = 20;

    private final int maxLen;

    private final Map<String, List<Message>> messages = new ConcurrentHashMap<>();

    public ShortTermMemory() {
        this.maxLen = DEFAULT_MAX_LEN;
    }

    public ShortTermMemory(int maxLen) {
        this.maxLen = maxLen;
    }

    public synchronized void add(String sessionId, Message msg) {
        messages.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(msg);
        trimIfNeeded(sessionId);
    }

    public synchronized void addBatch(String sessionId, List<Message> msgs) {
        messages.computeIfAbsent(sessionId, k -> new ArrayList<>()).addAll(msgs);
        trimIfNeeded(sessionId);
    }

    public List<Message> get(String sessionId) {
        return messages.getOrDefault(sessionId, Collections.emptyList());
    }

    /**
     * 获取最近N条消息
     * @param sessionId 会话id
     * @param n 取最近多少条
     * @return 消息副本
     */
    public List<Message> getRecent(String sessionId, int n) {
        List<Message> all = messages.getOrDefault(sessionId, Collections.emptyList());
        if (all.size() <= n) {
            return new ArrayList<>(all);
        }
        return new ArrayList<>(all.subList(all.size() - n, all.size()));
    }

    public void clear(String sessionId) {
        messages.remove(sessionId);
    }

    /**
     * 窗口裁剪逻辑：超过maxLen，丢弃最早消息，保留尾部maxLen条
     * @param sessionId 会话id
     */
    private void trimIfNeeded(String sessionId) {
        List<Message> msgs = messages.get(sessionId);
        if (msgs != null && msgs.size() > maxLen) {
            List<Message> trimmed = new ArrayList<>(msgs.subList(msgs.size() - maxLen, msgs.size()));
            messages.put(sessionId, trimmed);
        }
    }


}
