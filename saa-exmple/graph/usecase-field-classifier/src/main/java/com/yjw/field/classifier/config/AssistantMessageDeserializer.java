
package com.yjw.field.classifier.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssistantMessageDeserializer extends JsonDeserializer<AssistantMessage> {

    @Override
    public AssistantMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        JsonNode node = p.getCodec().readTree(p);

        String text = node.get("text").asText();

        Map<String, Object> metadata = new HashMap<>();
        JsonNode metadataNode = node.get("metadata");
        if (metadataNode != null && metadataNode.isObject()) {
            metadata = p.getCodec().treeToValue(metadataNode, Map.class);
        }

        List<ToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = node.get("toolCalls");
        if (toolCallsNode != null && toolCallsNode.isArray() && toolCallsNode.size() > 1) {
            JsonNode array = toolCallsNode.get(1);
            for (JsonNode item : array) {
                String id = item.get("id").asText();
                String type = item.get("type").asText();
                String name = item.get("name").asText();
                String arguments = item.get("arguments").asText();
                toolCalls.add(new ToolCall(id, type, name, arguments));
            }
        }

        return AssistantMessage.builder()
            .content(text)
            .properties(metadata)
            .toolCalls(toolCalls)
            .build();
    }
}
