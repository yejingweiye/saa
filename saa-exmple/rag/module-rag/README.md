## 1. RAG 拦截器
| 组件 | 特点 | 适用场景 |
|---|---|---|
| `QuestionAnswerAdvisor` | 简单封装，配置少；只有 `vectorStore`、`topK`、阈值；固定 prompt 模板；扩展能力弱 | 快速上手、简单 RAG 原型 |
| `RetrievalAugmentationAdvisor` | 模块化流水线：查询改写、查询扩展、检索、文档合并、重排过滤、Prompt 组装全部可以插拔替换；支持空上下文兜底、动态 filter | 生产级复杂 RAG，多路召回、重排序、查询改写等高级 RAG 流程 |


### 内部流水线执行顺序
#### before 阶段（请求发送给大模型之前执行）
1. **queryTransformers 查询转换器**：改写用户问题（纠错、翻译、基于对话历史重写问题）
2. **queryExpander 查询扩展器**：把 1 个问题扩展成多条，做多查询召回（Multi‑Query）
3. **documentRetriever 文档检索器**：调用向量库（ES/Milvus）做向量检索，拿到 Document 列表；可以传入 filter 过滤条件（例如前面的`location == 'North Pole'`元数据过滤）
4. **documentJoiner 文档合并器**：合并多路查询返回的文档，做去重
5. **documentPostProcessors 文档后处理器**：重排序 Rerank、过滤、截断、摘要，优化检索结果
6. **queryAugmenter 查询增强器**：把处理完成的上下文文档拼入 Prompt 模板，组装成最终发给大模型的请求

#### after 阶段（大模型返回结果之后执行）
- 把检索出来的文档元数据附加到响应对象，方便返回溯源引用。

## 2. 组成部分

### 2.1 Pre-Retrieval

> 增强和转换用户输入，使其更有效地执行检索任务，解决格式不正确的查询、query 语义不清晰、或不受支持的语言等。

1. **QueryAugmenter 查询增强**：使用附加的上下文数据信息增强用户 query，提供大模型回答问题时的必要上下文信息；
2. **QueryTransformer**：查询改写：因为用户的输入通常是片面的，关键信息较少，不便于大模型理解和回答问题。因此需要使用 prompt 调优手段或者大模型改写用户 query；
3. **QueryExpander**：查询扩展：将用户 query 扩展为多个语义不同的变体以获得不同视角，有助于检索额外的上下文信息并增加找到相关结果的机会。

### 2.2 Retrieval

> 负责查询向量存储等数据系统并检索和用户 query 相关性最高的 Document。

1. **DocumentRetriever**：检索器，根据 QueryExpander 使用不同的数据源进行检索，例如 搜索引擎、向量存储、数据库或知识图等；
2. **DocumentJoiner**：将从多个 query 和从多个数据源检索到的 Document 合并为一个 Document 集合；

### 2.3 Post-Retrieval

> 负责处理检索到的 Document 以获得最佳的输出结果，解决模型中的*中间丢失*和上下文长度限制等。

1. **DocumentRanker**：根据 Document 和用户 query 的相关性对 Document 进行排序和排名；
2. **DocumentSelector**：用于从检索到的 Document 列表中删除不相关或冗余文档；
3. **DocumentCompressor**：用于压缩每个 Document，减少检索到的信息中的噪音和冗余。

### 2.4 生成

生成用户 Query 对应的大模型输出。

## 3. 接口文档

```bash
curl -X POST http://127.0.0.1:10014/module-rag/rag/memory/123 \
  -H "Content-Type: application/json" \
  -d '{"prompt": "去北极冒险的角色都有谁？"}'
```


```bash
curl -X POST http://127.0.0.1:10014/module-rag/rag/compression/123 \
  -H "Content-Type: application/json" \
  -d '{"prompt": "他们去了哪些地方？"}'
```