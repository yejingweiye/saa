# RAG ETL Pipeline
## ETL 对应的逻辑代码
一个常见 RAG 链路是：
PDF -> Document -> split -> format -> enrich metadata -> embed -> vector store -> retrieval

- ReaderController.java [PDF -> Document ]
- TransformerController.java：[split -> format -> enrich metadata]
- WriterController.java [ embed -> vector store -> retrieval]

## 多格式文档
- txt
- json
- md
- pdf
- html

## TokenTextSplitter 参数说明

| 参数 | 含义 | 单位 | 注意点 |
|------|------|------|--------|
| `withChunkSize(800)` | 目标块 Token 数，核心参数。期望每个切块大概多少 token，属于理想目标，遇到段落边界允许小幅浮动。 | Token | Embedding 模型有输入上限；中文 1token≈1.5‑2 汉字。 |
| `withMinChunkSizeChars(350)` | 单个切块的最小字符数。切完如果块字符低于该值，会尝试和下一段合并，避免产出语义破碎的小块。 | 字符 (非 token) | 中文不要设置过大，否则块会严重膨胀。 |
| `withMinChunkLengthToEmbed(5)` | 切分完成后过滤阈值。块字符小于该值直接丢弃，不生成 Document，不会写入向量库。 | 字符 | 用于过滤空行、单独标点、换行碎片。 |
| `withMaxNumChunks(10000)` | 单份原始文档最多产出切块数量。超过该数量直接截断，作为保护阈值，防止超大文档生成海量 chunk 打满向量库。 | 个数 | 普通文档几乎不会触达。 |
| `withKeepSeparator(true)` | 是否保留切分用的分隔符（换行、段落空行等）。 | 布尔值 | true保留换行，可读性好；false丢弃分隔符，文本全部拼接。 |

  