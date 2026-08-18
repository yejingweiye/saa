

package com.yjw.bailian;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BailianAgentRagExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(BailianAgentRagExampleApplication.class, args);
	}

    @Bean
    public DashScopeAgentApi dashscopeAgentApi(DashScopeConnectionProperties connectionProperties) {
        return DashScopeAgentApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .build();
    }
}
