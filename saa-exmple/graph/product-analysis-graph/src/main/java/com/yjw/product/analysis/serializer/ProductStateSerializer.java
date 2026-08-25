
package com.yjw.product.analysis.serializer;

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

/**
 * 自定义 State 序列化器，基于 Jackson 启用默认类型信息，用于持久化/恢复 OverAllState
 * 中的自定义对象（如 Product），并在序列化时排除 null 值。
 */
public class ProductStateSerializer extends PlainTextStateSerializer {

    private final ObjectMapper mapper;

    /**
     * @param stateFactory 用于从原始 Map 重建 OverAllState 的工厂
     */
    public ProductStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
        super(stateFactory);
        this.mapper = new ObjectMapper();
        // Enable default typing to handle custom objects like Product
        this.mapper.activateDefaultTyping(
                this.mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        // Exclude null values from serialization
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 将 state 数据序列化为 JSON 字符串写入输出流。
     *
     * @param data state 数据 Map
     * @param out  序列化输出流
     * @throws IOException 序列化失败时抛出
     */
    @Override
    public void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
        String json = mapper.writeValueAsString(data);
        out.writeUTF(json);
    }

    /**
     * 从输入流读取 JSON 字符串并反序列化为 state 数据 Map。
     *
     * @param in 序列化输入流
     * @return 反序列化后的 state 数据 Map
     * @throws IOException 反序列化失败时抛出
     */
    @Override
    public Map<String, Object> readData(ObjectInput in) throws IOException {
        String json = in.readUTF();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 通过序列化-反序列化深拷贝一个 state。
     *
     * @param state 待拷贝的 state
     * @return 深拷贝后的新 state
     * @throws IOException 序列化或反序列化失败时抛出
     */
    @Override
    public OverAllState cloneObject(OverAllState state) throws IOException {
        String json = mapper.writeValueAsString(state.data());
        Map<String, Object> rawMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        return stateFactory().apply(rawMap);
    }
}
