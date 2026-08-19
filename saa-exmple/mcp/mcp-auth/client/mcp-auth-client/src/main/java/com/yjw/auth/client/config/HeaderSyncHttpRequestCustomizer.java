package com.yjw.auth.client.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;

public class HeaderSyncHttpRequestCustomizer implements McpSyncHttpClientRequestCustomizer {

    // 用于存放需要追加到 HTTP 请求头中的字段
    // 例如：Authorization、X-User-Id、tenant-id 等
    private final Map<String, String> headers;

    public HeaderSyncHttpRequestCustomizer(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public void customize(HttpRequest.Builder builder, String method, URI endpoint, String body, McpTransportContext context) {
        // 把 headers 中的每个键值对都追加到请求头中
        // 例如：Authorization: Bearer xxx
        // 例如：X-User-Id: 123
        headers.forEach(builder::header);
    }
}