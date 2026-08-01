package com.yjw.authsdk.resource.config;

import com.yjw.authsdk.resource.interceptors.FeignRelayUserInterceptor;
import feign.Feign;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FeignRelayUserAutoConfiguration
 * 使用场景：
 * - 当应用使用Feign进行服务间调用，并且需要在请求中携带用户信息时，可以使用该配置类。
 * @author yjw
 */
@Configuration
@ConditionalOnClass(Feign.class)
public class FeignRelayUserAutoConfiguration {

    @Bean
    public FeignRelayUserInterceptor feignRelayUserInterceptor(){
        return new FeignRelayUserInterceptor();
    }
}
