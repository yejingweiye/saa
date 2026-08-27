
package com.yjw.field.classifier.config;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class JsonStateSerializerWithTypeInfo extends PlainTextStateSerializer {

    private final ObjectMapper mapper;

    public JsonStateSerializerWithTypeInfo(AgentStateFactory<OverAllState> stateFactory, ObjectMapper mapper) {
        super(stateFactory);
        this.mapper = mapper;
    }

    public JsonStateSerializerWithTypeInfo(AgentStateFactory<OverAllState> stateFactory) {
        super(stateFactory);
        this.mapper = new ObjectMapper();
        this.mapper.activateDefaultTyping(
                this.mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String serialize(OverAllState state) throws IOException {
        return mapper.writeValueAsString(state.data());
    }

    public OverAllState deserialize(String data) throws IOException {
        Map<String, Object> rawMap = mapper.readValue(data, new TypeReference<>() {
        });
        return stateFactory().apply(rawMap);
    }

    @Override
    public OverAllState cloneObject(OverAllState state) throws IOException {
        String json = serialize(state);
        return deserialize(json);
    }

    @Override
    public void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
        String json = mapper.writeValueAsString(data);
        out.writeUTF(json);
    }

    @Override
    public Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
        String json = in.readUTF();
        return mapper.readValue(json, new TypeReference<>() {
        });
    }
}
