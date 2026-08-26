
package com.yjw.multiagent.graph.openmanus;
import com.yjw.multiagent.graph.openmanus.tool.GoogleSearch;
import com.yjw.multiagent.graph.openmanus.tool.PlanningTool;
import com.yjw.multiagent.graph.openmanus.tool.PythonExecute;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * 当前 Bean 中的工具配置尚未启用。我们仍然沿用之前 OpenManus 实现版本里的手动注册方式。
 */

@Configuration
public class OpenmanusAutoConfiguration {



	@Bean(name = "googleSearchFunction")
	@ConditionalOnMissingBean
	@Description(GoogleSearch.description)
	public GoogleSearch googleSearchFunction() {
		return new GoogleSearch();
	}

	@Bean(name = "planningToolFunction")
	@ConditionalOnMissingBean
	@Description(PlanningTool.description)
	public PlanningTool planningToolFunction() {
		return new PlanningTool();
	}

	@Bean(name = "pythonExecuteFunction")
	@ConditionalOnMissingBean
	@Description(PythonExecute.description)
	public PythonExecute pythonExecuteFunction() {
		return new PythonExecute();
	}

}
