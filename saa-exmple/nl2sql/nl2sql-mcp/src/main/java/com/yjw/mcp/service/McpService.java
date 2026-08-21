
package com.yjw.mcp.service;

import com.alibaba.cloud.ai.service.analytic.AnalyticNl2SqlService;
import com.alibaba.cloud.ai.service.simple.SimpleNl2SqlService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McpService {

	@Autowired
	private AnalyticNl2SqlService nl2SqlService;

	@Autowired
	private SimpleNl2SqlService simpleNl2SqlService;

	/**
	 * 从数据库中获取问题所需要的数据
	 * @return 从数据库中获取问题所需要的数据
	 */
	@Tool(description = "从数据库中获取问题所需要的数据")
	public String nl2Sql(String input) throws Exception {
		String sql = nl2SqlService.nl2sql(input);
		return nl2SqlService.executeSql(sql);
	}

	@Tool(description = "使用内存向量库从数据库中获取问题所需要数据")
	public String simpleNl2Sql(String input) throws Exception {
		String sql = simpleNl2SqlService.nl2sql(input);
		return simpleNl2SqlService.executeSql(sql);
	}

}
