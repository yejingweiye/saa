
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



