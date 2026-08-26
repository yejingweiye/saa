package com.yjw.multiagent.graph.openmanus.tool;

import com.yjw.multiagent.graph.openmanus.tool.support.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.function.BiFunction;

public class Summary implements BiFunction<String, ToolContext, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(Summary.class);

	private static final String PARAMETERS = """
			{
			  "type" : "object",
			  "properties" : {
			    "summary" : {
			      "type" : "string",
			      "description" : "The output of current step, better make a summary."
			    }
			  },
			  "required" : [ "summary" ]
			}
			""";

	private static final String name = "summary";

	private static final String description = "Record the summary of current step.";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback(String conversationId) {
		return FunctionToolCallback.builder(name, new Summary(conversationId))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	private String conversationId;

	public Summary(String conversationId) {
		this.conversationId = conversationId;
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("Summary toolInput:{}", toolInput);
		return new ToolExecuteResult(toolInput);
	}

	@Override
	public ToolExecuteResult apply(@ToolParam(description = PARAMETERS) String s, ToolContext toolContext) {
		// chatMemory.add(conversationId, toolContext.getToolCallHistory());
		return run(s);
	}

}
