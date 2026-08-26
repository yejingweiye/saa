package com.yjw.multiagent.graph.openmanus;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson.JSON;
import com.yjw.multiagent.graph.openmanus.tool.Plan;
import com.yjw.multiagent.graph.openmanus.tool.PlanningTool;

import java.util.Map;
import java.util.Optional;

/**
 * 监督者节点，基于规划工具维护的计划驱动多步任务执行：
 * 记录当前步骤输出、生成下一步执行提示词，并判断任务是否完成。
 */
public class SupervisorAgent implements NodeAction {

    private final PlanningTool planningTool;

    /**
     * @param planningTool 用于获取和更新执行计划的工具
     */
    public SupervisorAgent(PlanningTool planningTool) {
        this.planningTool = planningTool;
    }

    /**
     * 执行监督者节点逻辑：更新当前步骤状态，并生成下一步执行提示词。
     *
     * @param t 工作流当前状态
     * @return 更新后的状态键 {@code step_prompt}（下一步提示词）
     * @throws Exception 状态读取或计划解析失败时抛出
     */
    /**
     * OverAllState{data={input=帮我查询阿里巴巴近一周的股票信息, plan=```json
     * {
     * 	"planId": "G_47c34bd7-d9c6-4267-8cc2-3644142427c1",
     * 	"steps": [
     * 		"1. 查找可靠的金融数据来源（例如雅虎财经、东方财富网等）",
     * 		"2. 访问该网站并搜索阿里巴巴（股票代码：BABA 或 09988.HK）",
     * 		"3. 定位并提取近七天的股价数据（开盘价、收盘价、最高价、最低价、成交量）",
     * 		"4. 整理数据为结构化格式（如表格或JSON）",
     * 		"5. 输出结果供用户查看"
     * 	]
     * }
     * ```}, resume=false, humanFeedback=null, interruptMessage='null'}
     */
    @Override
    public Map<String, Object> apply(OverAllState t) throws Exception {

        String planStr = (String) t.value("plan").orElseThrow();

        Plan tempPlan = parsePlan(planStr);

        // todo
        Plan plan = planningTool.getGraphPlan(tempPlan.getPlan_id());

        Optional<Object> optionalOutput = t.value("step_output");

        if (optionalOutput.isPresent()) {
            String finalStepOutput = String.format("This is the final output of step %s:\n %s", plan.getCurrentStep(),
                    optionalOutput.get());
            plan.updateStepStatus(plan.getCurrentStep(), finalStepOutput);
        }

        String promptForNextStep;
        if (!plan.isFinished()) {
            promptForNextStep = plan.nextStepPrompt();
        }
        else {
            promptForNextStep = "Plan completed.";
        }

        return Map.of("step_prompt", promptForNextStep);


    }

    /**
     * 判断计划是否完成：若下一步提示为 "Plan completed." 则写入最终输出并返回 "end"，否则返回 "continue"。
     *
     * @param state 工作流当前状态
     * @return "end" 表示计划完成，否则 "continue"
     */
    public String think(OverAllState state) {

        String nextPrompt = (String) state.value("step_prompt").orElseThrow();

        if (nextPrompt.equalsIgnoreCase("Plan completed.")) {
            state.updateState(Map.of("final_output", state.value("step_output").orElseThrow()));
            return "end";
        }

        return "continue";
    }

    private Plan parsePlan(String planJson) {
        planJson = removeMarkdownCodeBlockSyntax(planJson);
        return JSON.parseObject(planJson, Plan.class);
    }

    /**
     * 移除字符串中的Markdown代码块标记（```json 和 ```） 如果字符串不包含这些标记，则返回原始字符串
     * @param input 可能包含Markdown代码块标记的字符串
     * @return 去除了代码块标记的字符串
     */
    public static String removeMarkdownCodeBlockSyntax(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 去除开头的 ```json 或 ```任何语言
        String result = input.trim();
        if (result.startsWith("```")) {
            int firstLineEnd = result.indexOf('\n');
            if (firstLineEnd != -1) {
                result = result.substring(firstLineEnd).trim();
            }
        }

        // 去除结尾的 ```
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3).trim();
        }

        return result;
    }
}
