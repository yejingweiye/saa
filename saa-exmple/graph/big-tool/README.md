
## 全自动调用工具的工作流   start->tools->calculate_agent->end
- tools 分析意图，从知识库找出相关的工具类方法，把（工具类方法和用户输入）传给下一个节点
- calculate_agent，把工具类给到LLM 模型，然后根据用户信息调起工具类方法
- 返回结果

