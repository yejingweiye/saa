
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

## multiagent-openmanus
多agent:规划总纲agent->协调监督每条计划进度agent->执行每条计划agent

## human-node
interruptBefore 模式 已知中断点编译时确定

## dynamic-interrupt-human-node
InterruptionMetadata 模式 可根据状态动态中断

## parallel-node
并行节点

## parallel-stream-node
注重流式处理

## stream-node
注重流式处理节点

## workflow-writing-assistant
写作助手

## usecase-field-classifier
字段敏感词分类
1. 生活类比

想象公司门口有个「数据安检台」：业务方递过来一个字段名（比如「身份证号」），要判断它能不能用、属于哪个分类、什么安全级别。

- 先过一道敏感词安检——命中黑名单直接拒收；
- 没问题的交给专家（LLM），专家查手册（RAG 知识库）后给出「分类路径 + 级别 + 理由」；
- 结论要人（合规专员）复核——同意就盖章归档（存数据库），不同意就打回重判（附上修改意见）；
- 全程像流水线一样走，每一步的产出都实时播报（SSE）。



