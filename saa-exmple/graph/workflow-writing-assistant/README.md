##  ComplexSupportGraphBuilder.java

这是一个纯线性链式图，从 START 一路串到 END，没有分支、没有条件边：

START
│
▼
extractDocs     文档抽取（读 data/manual.txt）              → docs
│
▼
parseParams     LLM 参数抽取（ticketId, priority）           → parameterParsing_output
│
▼
classify        LLM 问题分类（售后/技术支持/投诉/咨询）        → classifier_output
│
▼
retrieveDocs    向量库检索（用分类结果做 query）             → retrieved_docs
│
▼
syncTicket      HTTP 调用工单接口（GET mock）               → http_response
│
▼
invokeLLM       LLM 生成客服回复（基于 http_response）        → llm_response
│
▼
invokeTool      工具调用（sendEmail / updateCRM）            → tool_result
│
▼
humanReview     人工审核（条件判断是否中断）                 → answer
│
▼
finalAnswer     输出答案（{{answer}}）
│
▼
END

各节点逻辑

┌──────────────┬────────────────────────┬────────────────────────────────────┬────────────────────────────────────────────────────┐
│     节点     │          类型          │            输入 → 输出             │                        作用                        │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ extractDocs  │ DocumentExtractorNode  │ 读文件 → docs                      │ 从本地文档提取内容                                 │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ parseParams  │ ParameterParsingNode   │ input → parameterParsing_output    │ LLM 把工单文本抽成结构化参数（ticketId、priority） │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ classify     │ QuestionClassifierNode │ input → classifier_output          │ LLM 把问题归到 4 个类别之一                        │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ retrieveDocs │ KnowledgeRetrievalNode │ classifier_output → retrieved_docs │ 以分类结果为 query 做向量检索（topK=5，阈值 0.5）  │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ syncTicket   │ HttpNode               │ → http_response                    │ GET 调 mock 工单接口                               │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ invokeLLM    │ LlmNode                │ http_response → llm_response       │ 基于接口返回生成客服回复                           │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ invokeTool   │ ToolNode               │ llm_response → tool_result         │ 让 LLM 决定是否调用 sendEmail/updateCRM 工具       │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ humanReview  │ HumanNode              │ tool_result → answer               │ 唯一有特殊逻辑的节点                               │
├──────────────┼────────────────────────┼────────────────────────────────────┼────────────────────────────────────────────────────┤
│ finalAnswer  │ AnswerNode             │ answer                             │ 模板输出最终答案                                   │
└──────────────┴────────────────────────┴────────────────────────────────────┴────────────────────────────────────────────────────┘

关键点：humanReview 的人工审核逻辑

这个节点不是无条件中断的，它带一个条件：

new HumanNode("conditioned",
st -> st.value("tool_result").map(r -> r.toString().contains("ERROR")).orElse(false),
st -> Map.of("answer", st.value("tool_result").orElse("").toString()));

- 第一个 lambda 是中断条件：只有当 tool_result 里包含 "ERROR" 时才触发人工介入；否则直接放行。
- 第二个 lambda 是恢复后的状态合并：把 tool_result 作为 answer 写回 state，继续走到 finalAnswer。

换句话说：工具执行出错 → 停下来等人工处理（需要配合 interruptBefore/resume 机制）；工具正常 → 自动把结果当作答案继续输出。