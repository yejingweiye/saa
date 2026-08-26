
package com.yjw.multiagent.graph.openmanus.tool;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public class Builder {

	public static List<ToolCallback> getToolCallList() {
		return List.of(PlanningTool.getFunctionToolCallback());
	}

	public static List<ToolCallback> getManusAgentToolCalls() {
		return List.of(GoogleSearch.getFunctionToolCallback(), BrowserUseTool.getFunctionToolCallback(),
				FileSaver.getFunctionToolCallback(), PythonExecute.getFunctionToolCallback());
	}

	public static List<ToolCallback> getFunctionCallbackList() {
		return List.of(PlanningTool.getFunctionToolCallback());
	}

	public static List<ToolCallback> getManusAgentFunctionCallbacks() {
		return List.of(GoogleSearch.getFunctionToolCallback(), BrowserUseTool.getFunctionToolCallback(),
				FileSaver.getFunctionToolCallback(), PythonExecute.getFunctionToolCallback());
	}

}
