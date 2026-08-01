package com.yjw.authsdk.gateway.config;

import com.yjw.authsdk.gateway.util.AuthUtil;
import com.yjw.authsdk.gateway.util.JwtSignerHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AuthAutoConfiguration {

    // JWT 签名持有
    @Bean
    @ConditionalOnClass(DiscoveryClient.class)
    public JwtSignerHolder jwtSignerHolder(DiscoveryClient discoveryClient){
        return new JwtSignerHolder(discoveryClient);
    }

    // 网关鉴权
    @Bean
    public AuthUtil authUtil(JwtSignerHolder jwtSignerHolder, StringRedisTemplate stringRedisTemplate){
        return new AuthUtil(jwtSignerHolder, stringRedisTemplate);
    }
}
