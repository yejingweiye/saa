package com.yjw.multiagent.graph.openmanus.tool.support;


import com.yjw.multiagent.graph.openmanus.tool.Plan;

public class PlanToolExecuteResult extends ToolExecuteResult {

	private String id;

	private Plan plan;

	public PlanToolExecuteResult(String output, String id) {
		super(output);
		this.id = id;
	}

	public PlanToolExecuteResult(Plan plan, String output, String id) {
		super(output);
		this.id = id;
		this.plan = plan;
	}

	String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}

}
