
package com.yjw.writing.assistant.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/write")
public class WritingAssistantController {

	private final CompiledGraph compiledGraph;

	@Autowired
	public WritingAssistantController(@Qualifier("writingAssistantGraph") StateGraph writingAssistantGraph)
			throws GraphStateException {
		this.compiledGraph = writingAssistantGraph.compile();
	}

	/**
	 * 调用写作助手流程图 示例请求：GET /write?text=今天我去了西湖，天气特别好，感觉特别开心
	 */
	@GetMapping
	public Map<String, Object> write(@RequestParam("text") String inputText) throws GraphRunnerException {
		var resultFuture = compiledGraph.call(Map.of("original_text", inputText));
		var result = resultFuture.get();
		return result.data();
	}

}
