
## 数据库配置

### PostgreSQL + pgvector 设置

首先需要在 PostgreSQL 数据库中创建必要的表和索引：

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS vector_store (
id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
content text,
metadata json,
embedding vector(1536)
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
```

## 文档导入
文档上传->解析内容->文本分块(TokenTextSplitter)->每个分文档元数据绑定fileId->向量嵌入->存储到PGVector

## 文档检索与问答
用户问题->向量检索->重排序->生成回答

## 文档管理
文档向量删除

## 系统词模版
上下文占位符，回答规则约束，知识边界设定，从而避免幻觉

## topK(5)向量粗召 → Rerank 精排过滤
### 阶段 1：向量库相似度粗召回（VectorStore#search）
1. 将用户问题向量化，生成 query 向量
2. 在向量数据库做余弦相似度检索，按照向量相似度分数从高到低排序
3. 取前 topK=5 条文档，这一步只看向量相似度，还没走 rerank 模型
   ⚠️ 注意：向量相似度 ≠ 语义真实相关性！向量分高，实际语义可能不匹配。
   输出：候选集合 A，大小固定 5 条文档。
### 阶段 2：Rerank 重排序模型精排 + 阈值过滤
Advisor 拿到上面的 5 条文档，交给rerankModel：
1. 输入：query + [doc1,doc2,doc3,doc4,doc5]
2. rerank 模型对每一篇文档和用户问题做语义相关性打分，分数区间一般 0~1
3. 按照 rerank 分数重新降序排序
4. 执行阈值过滤：分数 <0.1 的文档直接被丢弃

### 几种结果案例
1. 5 条全部≥0.1 → 保留全部 5 条，按 rerank 分数重新排序，送入 prompt
2. 5 条里面 2 条分数 0.03、0.08（小于 0.1）→ 过滤丢弃，最终只剩 3 条文档进上下文
3. 如果 5 条全部低于 0.1 → 最终0 条文档，RAG 没有检索到有效资料，大模型直接凭自身知识回答

