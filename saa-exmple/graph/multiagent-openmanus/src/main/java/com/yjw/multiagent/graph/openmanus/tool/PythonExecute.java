package com.yjw.multiagent.graph.openmanus.tool;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yjw.multiagent.graph.openmanus.tool.support.CodeExecutionResult;
import com.yjw.multiagent.graph.openmanus.tool.support.CodeUtils;
import com.yjw.multiagent.graph.openmanus.tool.support.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public class PythonExecute implements BiFunction<String, ToolContext, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(PythonExecute.class);

	private UUID uuid = UUID.randomUUID();

	private Boolean arm64 = true;

	public static final String LLMMATH_PYTHON_CODE = "import sys; import math; import numpy as np; import numexpr as ne; input = '%s'; res = ne.evaluate(input); print(res)";

    public static final String PARAMETERS = """
        {
            "type": "object",
            "properties": {
                "code": {
                    "type": "string",
                    "description": "待执行的Python代码"
                }
            },
            "required": ["code"]
        }
        """;

    private static final String name = "python_execute";

    public static final String description = """
        执行Python代码字符串。注意：仅print打印输出内容可见，函数返回值不会被捕获，请使用print语句输出运行结果。
        """;


    public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, new PythonExecute())
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("PythonExecute toolInput:{}", toolInput);
		Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
		});
		String code = (String) toolInputMap.get("code");
		// String result = PythonUtils.invokePythonCodeWithArch(code, arm64);
		CodeExecutionResult codeExecutionResult = CodeUtils.executeCode(code, "python",
				"tmp_" + uuid.toString() + ".py", arm64, new HashMap<>());
		String result = codeExecutionResult.getLogs();
		return new ToolExecuteResult(result);
	}

	public Boolean getArm64() {
		return arm64;
	}

	public void setArm64(Boolean arm64) {
		this.arm64 = arm64;
	}

	@Override
	public ToolExecuteResult apply(@ToolParam(description = PARAMETERS) String s, ToolContext toolContext) {
		return run(s);
	}

}
