package com.yjw.react.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class ReactAutoconfiguration {

    /**
     * internalToolExecutionEnabled = true（自动执行工具）
     * 用户：深圳今天天气怎么样？
     * ↓
     * 发给大模型，大模型返回：需要调用工具 getWeather(city="深圳")
     * ↓
     * ✅SpringAI内部自动：
     *    1.解析tool_call
     *    2.调用getWeather("深圳")拿到结果："深圳 28℃，多云"
     *    3.把工具结果塞回给大模型
     *    4.大模型整理自然语言回答
     * ↓
     * 业务代码直接拿到最终回答："深圳今天28摄氏度，天气多云。"
     */

    /**
     * internalToolExecutionEnabled = false（关闭自动执行，透传原始消息）
     * 用户：深圳今天天气怎么样？
     * ↓
     * 发给大模型，大模型返回tool_call：调用getWeather(city="深圳")
     * ↓
     * ❗SpringAI**不会执行工具，直接把tool_call消息原样返回给上层**
     * ↓
     * 上层（SAA Graph的AgentToolNode / 你的业务代码）：
     *   1.识别到消息是ToolCall
     *   2.手动执行getWeather("深圳")
     *   3.组装ToolMessage，放回messages消息列表
     *   4.再次交给大模型继续推理
     * ↓
     * 拿到最终回答
     */
    @Bean
    public ReactAgent normalReactAgent(ChatModel chatModel, ToolCallbackResolver resolver) throws GraphStateException {
        ChatClient chatClient = ChatClient
                .builder(chatModel)
                .defaultToolNames("getWeatherFunction")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(OpenAiChatOptions.builder()
                        .internalToolExecutionEnabled(false) // 工具调用
                        .build())
                .build();

        return ReactAgent.builder()
                .name("React Agent Demo")
                .chatClient(chatClient)
                .resolver(resolver)
                .toolNames("getWeatherFunction")
                .build();
    }

    // 从agent 获取图注入
    @Bean
    public CompiledGraph reactAgentGraph(@Qualifier("normalReactAgent") ReactAgent reactAgent)
    throws GraphStateException{

        // getCompiledGraph() 只返回内部缓存的 compiledGraph 字段（默认 null，懒编译）
        // getAndCompileGraph() 才会真正触发 StateGraph.compile() 并缓存结果
        CompiledGraph compiledGraph = reactAgent.getAndCompileGraph();

        GraphRepresentation graphRepresentation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
        System.out.println("\n\n");
        System.out.println(graphRepresentation.content());
        System.out.println("\n\n");

        return compiledGraph;
    }

    // 设置连接，避免超时
    @Bean
    public RestClient.Builder createRestClient(){
        // 2. 创建 RequestConfig 并设置超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(10, TimeUnit.MINUTES)) // 设置连接超时
                .setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
                .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES))
                .build();

        // 3. 创建 CloseableHttpClient 并应用配置
        HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

        // 4. 使用 HttpComponentsClientHttpRequestFactory 包装 HttpClient
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // 5. 创建 RestClient 并设置请求工厂
        return RestClient.builder().requestFactory(requestFactory);
    }


}
