## 示例场景

本项目包含两个典型业务场景：

### 1. 订单审批场景

当订单金额超过 10000 元时，自动中断流程等待人工审批。

**工作流程**：
START → order_approval → final_process → END ↓ (金额 > 10000) 中断，等待审批 ↓ (审批后) 继续执行

**核心实现**：

```java
public class OrderApprovalNode implements NodeAction, InterruptableAction {
    private static final double APPROVAL_THRESHOLD = 10000.0;

    @Override
    public Optional<InterruptionMetadata> interrupt(
            String nodeId, OverAllState state, RunnableConfig config) {

        // 检查是否已有审批反馈
        Optional<Object> feedback = config.metadata(
                RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY
        );
        if (feedback.isPresent()) {
            return Optional.empty(); // 已审批，继续执行
        }

        // 检查订单金额
        Double orderAmount = state.value("order_amount", 0.0);
        if (orderAmount > APPROVAL_THRESHOLD) {
            return Optional.of(
                    InterruptionMetadata.builder(nodeId, state)
                            .addMetadata("reason", "订单金额超过阈值，需要人工审批")
                            .addMetadata("order_amount", orderAmount)
                            .build()
            );
        }

        return Optional.empty();
    }
}
```

### 2. 敏感操作确认场景

执行敏感操作（如删除用户、修改系统配置）前需要人工确认。

**工作流程**：
START → sensitive_operation → final_process → END
↓ (敏感操作)
中断，等待确认
↓ (确认后)
继续执行

**核心实现**：

```java
public class SensitiveOperationNode implements NodeAction, InterruptableAction {
    private static final Set<String> SENSITIVE_OPERATIONS = Set.of(
            "delete_user", "delete_database", "modify_system_config"
    );


    @Override
    public Optional<InterruptionMetadata> interrupt(
            String nodeId, OverAllState state, RunnableConfig config) {

        // 检查是否为敏感操作
        String operation = state.value("operation", "");
        if (SENSITIVE_OPERATIONS.contains(operation)) {
            return Optional.of(
                    InterruptionMetadata.builder(nodeId, state)
                            .addMetadata("reason", "敏感操作需要人工确认")
                            .addMetadata("operation", operation)
                            .addMetadata("risk_level", getRiskLevel(operation))
                            .build()
            );
        }

        return Optional.empty();
    }
}
```

### 3. 执行流程

1. NodeExecutor 执行节点前
   ↓
2. 检查节点是否实现 InterruptableAction
   ↓
3. 调用 interrupt() 方法
   ↓
4. 返回值判断
   ├─ Optional.of(metadata) → 中断执行，保存状态
   └─ Optional.empty() → 继续执行，调用 apply() 方法

### 4. 恢复执行

中断后通过 HUMAN_FEEDBACK_METADATA_KEY 传递反馈信息恢复执行：

```java
Map<String, Object> feedback = Map.of("approved", true);

RunnableConfig resumeConfig = RunnableConfig.builder()
        .threadId(originalThreadId)
        .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
        .build();

compiledGraph.stream(null,resumeConfig);
```