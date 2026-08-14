package com.yjw.dashscope.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * 注册 DashScopeAudioTranscriptionApi bean
 */
@Configuration
public class AudioTranscriptionConfig {

    @Bean
    public DashScopeAudioTranscriptionApi dashScopeAudioTranscriptionApi(
            @Value("${spring.ai.dashscope.api-key}") String apiKey) {
        return DashScopeAudioTranscriptionApi.builder()
                .baseUrl("https://dashscope.aliyuncs.com")
                .apiKey(apiKey)
                .model(DashScopeModel.AudioModel.PARAFORMER_REALTIME_V2.getValue())
                .headers(new HttpHeaders())
                .restClientBuilder(RestClient.builder())
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();
    }
}
