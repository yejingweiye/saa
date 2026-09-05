package com.mirror.agent.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mirror.agent.agent.*;
import com.mirror.agent.memory.LongTermMemory;
import com.mirror.agent.memory.MySQLStore;
import com.mirror.agent.memory.ShortTermMemory;
import com.mirror.agent.rag.BM25Manager;
import com.mirror.agent.rag.MilvusStore;
import com.mirror.agent.rag.Reranker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction.node_async;

/**
 * 面试流程编排器 —— 用 Spring AI Alibaba Graph（StateGraph）把 6 阶段编排成有向图。
 * <p>
 * 图结构：
 * <pre>
 *   START → jd_analysis → resume_match → question_plan → interview
 *                                                            │
 *                                       (用户未作答即终止) ──┴── END
 *                                                            │
 *                                          weak_review → evaluation → review_plan → END
 * </pre>
 * 说明：graph 在执行节点前会对 OverAllState 做 Jackson 深拷贝快照，因此<strong>不能</strong>把
 * 回调（含会阻塞的 getUserAnswer）和业务对象塞进 state。这里让 graph 的 state 保持为空、只负责
 * 编排节点的执行顺序与条件分支；面试上下文（输入、各阶段产物、回调）放在一个 per-interview 的
 * {@link Ctx} 持有者里，由各节点闭包捕获共享。对外行为、回调时序、前端消息协议与顺序编排版本一致。
 */
@Slf4j
@Component
public class Orchestrator {

    private final JDAnalyzer jdAnalyzer; // 岗位描述
    private final ResumeMatcher resumeMatcher; // 简历匹配
    private final QuestionPlanner questionPlanner; // 问题规划
    private final Interviewer interviewer; // 面试
    private final Evaluator evaluator; // 评价
    private final ReviewPlanner reviewPlanner; // 复盘规划
    private final ShortTermMemory shortTermMem; // 短期记忆
    private final LongTermMemory longTermMem; // 长期记忆
    private final MilvusStore milvusStore; // Milvus 向量数据库
    private final BM25Manager bm25Manager; // BM25 文本检索
    private final Reranker reranker; // 重排序器
    private final MySQLStore mysqlStore; // MySQL 存储
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()); //  不加 `new JavaTimeModule()`，LocalDateTime直接抛出异常

    public Orchestrator(JDAnalyzer jdAnalyzer, ResumeMatcher resumeMatcher,
                        QuestionPlanner questionPlanner, Interviewer interviewer,
                        Evaluator evaluator, ReviewPlanner reviewPlanner,
                        ShortTermMemory shortTermMem, LongTermMemory longTermMem,
                        MilvusStore milvusStore, BM25Manager bm25Manager,
                        Reranker reranker, MySQLStore mysqlStore) {
        this.jdAnalyzer = jdAnalyzer;
        this.resumeMatcher = resumeMatcher;
        this.questionPlanner = questionPlanner;
        this.interviewer = interviewer;
        this.evaluator = evaluator;
        this.reviewPlanner = reviewPlanner;
        this.shortTermMem = shortTermMem;
        this.longTermMem = longTermMem;
        this.milvusStore = milvusStore;
        this.bm25Manager = bm25Manager;
        this.reranker = reranker;
        this.mysqlStore = mysqlStore;
    }

    /**
     * 执行完整面试流程：构建并编译一张 StateGraph（节点闭包捕获本次面试上下文），驱动它跑完。
     */
    public Session runInterview(String jdText, String resumeText, String userID,
                                InterviewCallbacks cb) throws Exception {
        Ctx c = new Ctx(jdText, resumeText, userID, cb);

        // state 留空（只放一个占位 key，不放业务对象，避免深拷贝序列化）
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> keys = new HashMap<>();
            keys.put("_", new ReplaceStrategy());
            return keys;
        };

        StateGraph graph = new StateGraph(keyStrategyFactory)
                .addNode("jd_analysis", node_async(s -> { jdAnalysis(c); return Map.of(); }))
                .addNode("resume_match", node_async(s -> { resumeMatch(c); return Map.of(); }))
                .addNode("question_plan", node_async(s -> { questionPlan(c); return Map.of(); }))
                .addNode("interview", node_async(s -> { interview(c); return Map.of(); }))
                .addNode("weak_review", node_async(s -> { weakReview(c); return Map.of(); }))
                .addNode("evaluation", node_async(s -> { evaluation(c); return Map.of(); }))
                .addNode("review_plan", node_async(s -> { reviewPlan(c); return Map.of(); }))
                .addEdge(START, "jd_analysis")
                .addEdge("jd_analysis", "resume_match")
                .addEdge("resume_match", "question_plan")
                .addEdge("question_plan", "interview")
                .addConditionalEdges("interview", edge_async(s -> afterInterview(c)),
                        Map.of("end", END, "continue", "weak_review"))
                .addEdge("weak_review", "evaluation")
                .addEdge("evaluation", "review_plan")
                .addEdge("review_plan", END);
        CompiledGraph compiledGraph = graph.compile();
        log.info("[Orchestrator] 面试流程 StateGraph 已编译，开始执行");
        compiledGraph.invoke(new HashMap<>());
        log.info("[Orchestrator] 面试流程 StateGraph 执行完成");

        return c.session;
    }

    // ============================================================
    // 各阶段节点（读写 Ctx，调回调推送前端）
    // ============================================================

    /** 阶段 1：JD 分析 */
    private void jdAnalysis(Ctx c) {
        c.session = new Session()
    }


    // ============================================================
    // 面试上下文持有者（不进入 graph state，避免被序列化）
    // ============================================================
    private static final class Ctx {
        final String jdText;
        final String resumeText;
        final String userID;
        final InterviewCallbacks cb;

        Session session;
        JDAnalysis jdAnalysis;
        Resume resume;
        ResumeMatchResult matchResult;
        QuestionPlan questionPlan;
        InterviewState interviewState;
        EvaluationReport report;
        ReviewPlan reviewPlan;
        boolean userTerminated;

        Ctx(String jdText, String resumeText, String userID, InterviewCallbacks cb) {
            this.jdText = jdText;
            this.resumeText = resumeText;
            this.userID = userID;
            this.cb = cb;
        }
    }


}
