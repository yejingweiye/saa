package com.tianji.aigc.memory;

import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.Map;

//采取自定义消息类的方式，避免使用springAI自带的消息类，序列化时有个getText() 无法拿到context属性的值，导致序列化失败

/**
 * {
 *
 *
 *     "messageType": "ASSISTANT",
 *     "metadata": {
 *         "finishReason": "STOP",
 *         "id": "257e98c4-dde3-9426-b3de-022fb9b3012e",
 *         "role": "ASSISTANT",
 *         "messageType": "ASSISTANT",
 *         "reasoningContent": ""
 *     }，
 *     "media": [],
 *     "toolCalls": [],
 *     "textContent": "请使用中文回答",
 *
 * }
 */
@Data
public class MyMessage {

    private String messageType;
    private Map<String, Object> metadata = Map.of();
    private List<Media> media = List.of();
    private List<AssistantMessage.ToolCall> toolCalls = List.of();
    private String textContent;
    private List<ToolResponseMessage.ToolResponse> toolResponses = List.of();
    private Map<String, Object> params = Map.of();

}
