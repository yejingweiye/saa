

package com.yjw.brk.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;



@AutoConfiguration
public class BailianAutoconfiguration {

	/**
	 * 百炼调用时需要配置 DashScope API，对 dashScopeApi 强依赖。
	 * @return
	 */
	@Bean
	public DashScopeApi dashScopeApi() {
		
		return DashScopeApi.builder().apiKey(System.getenv("${AI_DASHSCOPE_API_KEY}")).build();
	}

}
