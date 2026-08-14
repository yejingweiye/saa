package com.yjw.bailian;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;



@AutoConfiguration
@EnableConfigurationProperties({
		DashScopeConnectionProperties.class
})
public class BailianAutoconfiguration {

	/**
	 * 百炼调用时需要配置 DashScope API，对 dashScopeApi 强依赖。
	 * @return
	 */
	@Bean
	public DashScopeApi dashScopeApi(DashScopeConnectionProperties connectionProperties) {
		
		return DashScopeApi.builder().apiKey(connectionProperties.getApiKey()).build();
	}

}
