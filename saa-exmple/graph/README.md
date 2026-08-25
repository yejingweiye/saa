
## reflection 版本太旧不跑
## graph-observability-langfuse 基于图的langfuse监控系统
## react reactAgent 图调用工具节点
## chatflow chat待办工作流
```text
start->intent->chat/callSubGraph->mainReply->end
```
## workflow-review-classifier 工作流审查分类
```text
1.积极
积极->第一级分类节点->http 积极节点处理
2.消极
消极反馈->第一级分类节点->第二级分类节点->http 消极节点处理
```

## product-analysis-graph 产品分析图
输入产品信息->并行产生营销广告和挑出产品规格->合并json输出