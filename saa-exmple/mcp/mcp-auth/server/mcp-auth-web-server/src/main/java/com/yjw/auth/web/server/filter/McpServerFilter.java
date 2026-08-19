package com.yjw.auth.web.server.filter;

import com.yjw.auth.web.server.util.UserInfoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;

//  WebFlux 的过滤器，用于处理请求头中的 token 验证
@Component
public class McpServerFilter implements WebFilter {

    private static final String TOKEN_HEADER = "token-1";
    private static final String TOKEN_VALUE = "yjw-1";

    private static final Map<String,String> USER_INFO_MAP = Map.of(TOKEN_VALUE,"Fake_UserInfo");

    private static final Logger logger = LoggerFactory.getLogger(McpServerFilter.class);


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 获取请求头中的token值
        HttpHeaders headers = exchange.getRequest().getHeaders();
        // 打印所有请求头信息
        for (String headerName : headers.keySet()){
            logger.info("Header {}: {}", headerName, headers.getFirst(headerName));
        }

        String token = headers.getFirst(TOKEN_HEADER);
        // 检查token是否存在且值正确
        if (TOKEN_VALUE.equals(token)){
            logger.info("preHandle: 验证通过");
            logger.info("preHandle: 请求的URL: {}", exchange.getRequest().getURI());
            logger.info("preHandle: 请求的TOKEN: {}", token);
            UserInfoHolder.setUserInfo(USER_INFO_MAP.get(token)); // 设置用户信息
            // token验证通过，继续处理请求
            return chain.filter(exchange);
        }else{
            // token验证失败，返回401未授权错误
            logger.warn("Token验证失败: 请求的URL: {}, 提供的TOKEN: {}", exchange.getRequest().getURI(), token);
            logger.warn("要求的token为：{}", TOKEN_VALUE);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

    }
}
