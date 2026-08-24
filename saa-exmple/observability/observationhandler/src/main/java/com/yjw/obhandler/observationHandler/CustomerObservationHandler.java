package com.yjw.obhandler.observationHandler;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.springframework.ai.chat.observation.ChatModelObservationContext;

/**
 * 1. **过滤查找链路**：以后可以在追踪平台，按这个标签筛选出所有 AI 调用的链路，快速定位问题。
 * 2. **上下文信息**：点开这条调用详情，一眼就知道这是哪一类业务的 AI 请求。
 *
 * 在监控指标（Prometheus/Grafana）的作用（这才是 LowCardinality 标签最重要的价值）
 *
 * 加上这个标签后，**所有指标都会带上该维度，你可以按这个标签做分组统计**：
 * 举业务例子：
 * 标签改为 `biz.scene=rag_chat`
 *
 * 1. 统计 rag_chat 场景的每秒调用 QPS
 * 2. 看 rag_chat 场景 AI 接口的 P95 耗时
 * 3. 看 rag_chat 消耗了多少 token
 * 4. 针对 rag_chat 场景单独配置告警（错误率高就报警）
 *
 * >
 * > 如果是 `addHighCardinalityKeyValue`，标签只会出现在 Trace，**不会出现在 Prometheus 指标，不能做上面的统计和告警**。
 */
public class CustomerObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    // AI 请求**开始前**触发，Context 刚创建，可以往里面加自定义标签
    @Override
    public void onStart(ChatModelObservationContext context) {
        context.addLowCardinalityKeyValue(new KeyValue() {
            @Override
            public String getKey() {
                return "test.lowcardinality.Key";
            }

            @Override
            public String getValue() {
                return "test.lowcardinality.value";
            }
        });

        System.out.println("exec CustomerObservationHandler onStart function! ChatModelObservationContext: " + context.toString() );
    }

    // AI 调用结束（成功 / 异常）触发，此时 token、耗时、返回结果已经填充完毕
    @Override
    public void onStop(ChatModelObservationContext context) {
        System.out.println("exec CustomerObservationHandler onStop function! ChatModelObservationContext: " + context.toString() );

    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }
}
