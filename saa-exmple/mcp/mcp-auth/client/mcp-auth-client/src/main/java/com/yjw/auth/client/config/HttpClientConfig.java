package com.yjw.auth.client.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class HttpClientConfig {

    @Bean
    public McpSyncHttpClientRequestCustomizer mcpSyncHttpClientRequestCustomizer() {
        Map<String,String> headers = new HashMap<>();
        headers.put("token-1","yjw-1");
        headers.put("token-2","yjw-2");

        return new HeaderSyncHttpRequestCustomizer(headers);
    }

}
