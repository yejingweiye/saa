package com.yjw.multiagent.graph.openmanus;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.yjw.multiagent.graph.openmanus.tool.Builder;
import com.yjw.multiagent.graph.openmanus.tool.PlanningTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.yjw.multiagent.graph.openmanus.OpenManusPrompt.PLANNING_SYSTEM_PROMPT;
import static com.yjw.multiagent.graph.openmanus.OpenManusPrompt.STEP_SYSTEM_PROMPT;

@RestController
@RequestMapping("/manus")
public class OpenmanusController {

    private final ChatClient planningClient;

    private final ChatClient stepClient;

    private CompiledGraph compiledGraph;


    // 也可以使用如下的方式注入 ChatClient
    public OpenmanusController(ChatModel chatModel) throws GraphStateException{
        this.planningClient = ChatClient
                .builder(chatModel)
                .defaultSystem(PLANNING_SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultToolCallbacks(Builder.getToolCallList())
                .defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
                .build();

        this.stepClient = ChatClient.builder(chatModel)
                .defaultSystem(STEP_SYSTEM_PROMPT)
                .defaultToolCallbacks(Builder.getManusAgentToolCalls())// tools registered
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
                .build();

        initGraph();
    }

    public void initGraph() throws GraphStateException {
        OverAllStateFactory stateFactory = () -> {
            OverAllState state = new OverAllState();
            state.registerKeyAndStrategy("plan", new ReplaceStrategy());
            state.registerKeyAndStrategy("step_prompt", new ReplaceStrategy());
            state.registerKeyAndStrategy("step_output", new ReplaceStrategy());
            state.registerKeyAndStrategy("final_output", new ReplaceStrategy());

            return state;
        };


        SupervisorAgent supervisorAgent = new SupervisorAgent(PlanningTool.INSTANCE);

        // 思考 → 调用工具 → 获取结果，算作一轮迭代
        ReactAgent planningAgent = new ReactAgent("planningAgent", planningClient, Builder.getFunctionCallbackList(), 10);
        planningAgent.getAndCompileGraph().setMaxIterations(100);

        ReactAgent stepAgent = new ReactAgent("stepAgent", stepClient, Builder.getManusAgentFunctionCallbacks(), 10);
        stepAgent.getAndCompileGraph().setMaxIterations(100);

        StateGraph graph = new StateGraph(stateFactory)
                .addNode("planning_agent", planningAgent.asAsyncNodeAction("input", "plan"))
                .addNode("supervisor_agent", node_async(supervisorAgent))
                .addNode("step_executing_agent", stepAgent.asAsyncNodeAction("step_prompt", "step_output"))

                .addEdge(START, "planning_agent")
                .addEdge("planning_agent", "supervisor_agent")
                .addConditionalEdges("supervisor_agent", edge_async(supervisorAgent::think),
                        Map.of("continue", "step_executing_agent", "end", END))
                .addEdge("step_executing_agent", "supervisor_agent");

        this.compiledGraph = graph.compile();
        this.compiledGraph.setMaxIterations(100);

        GraphRepresentation graphRepresentation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML);
        System.out.println("\n\n");
        System.out.println(graphRepresentation.content());
        System.out.println("\n\n");

    }

    /**
     * ChatClient 简单调用
     */
    @GetMapping("/chat")
    public String simpleChat(String query) throws GraphRunnerException {

        return compiledGraph.invoke(Map.of("input", query)).get().data().toString();
    }
}
