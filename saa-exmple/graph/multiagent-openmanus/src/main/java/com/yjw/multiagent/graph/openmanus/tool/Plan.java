package com.yjw.multiagent.graph.openmanus.tool;

import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plan {

    private Map<String, String> stepStatus;

    private int currentStep = 0;

    private String task;

    private String plan_id;

    private List<String> steps;

    public Plan(String task, String planId, List<String> steps) {
        this.task = task;
        this.plan_id = planId;
        this.steps = steps;
        this.stepStatus = new HashMap<>();
    }

    public String getCurrentStep() {
        return String.valueOf(currentStep);
    }

    public String getPlan_id() {
        return plan_id;
    }

    public void updateStepStatus(String stepIndex, String status) {
        stepStatus.put(stepIndex, status);
    }

    public String nextStepPrompt() {
        String nextStepDescription = steps.get(currentStep);
        Map<String, Object> context = new HashMap<>();
        context.put("task", task);
        context.put("planWithSteps", steps);
        context.put("stepIndex", currentStep);
        context.put("nextStepDescription", nextStepDescription);
        context.put("stepStatus", stepStatus);

        currentStep++;

        String template = """
                任务：{task}
                
                请按照下面这份计划，依据顺序步骤完成该任务：
                {planWithSteps}
                
                你当前执行到计划的第 {stepIndex} 步，步骤内容：{nextStepDescription}。
                
                以下为前面步骤的执行结果，可作为上下文辅助你完成当前步骤：
                {stepStatus}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        return promptTemplate.render(context);
    }

    public String nextStep() {
        return steps.get(currentStep++);
    }

    public boolean isFinished() {
        return currentStep == steps.size();
    }

    void setPlan_id(String planId) {
        this.plan_id = planId;
    }

    void setSteps(List<String> steps) {
        this.steps = steps;
    }

}
