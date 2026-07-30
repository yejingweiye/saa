package com.yjw.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OrderApprovalNode implements AsyncNodeActionWithConfig, InterruptableAction {

    private static final Logger logger = LoggerFactory.getLogger(OrderApprovalNode.class);

    // 审核金额阈值
    private static final double APPROVAL_THRESHOLD = 10000.0;


    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        logger.info("OrderApprovalNode.apply() executing");

        String orderId = state.value("order_id", "UNKNOWN");
        Double orderAmount = state.value("order_amount", 0.0);
        boolean wasApproved = state.value("approved", false);

        Map<String, Object> result = new HashMap<>();
        if (orderAmount > APPROVAL_THRESHOLD) {
            if(wasApproved){
                logger.info("Order {} approved, processing", orderId);
                result.put("order_status", "approved_and_processing");
                result.put("message", String.format("订单 %s 已获批准，正在处理中...", orderId));
            }else {
                logger.info("Order {} rejected", orderId);
                result.put("order_status", "rejected");
                result.put("message", String.format("订单 %s 审批被拒绝", orderId));
            }
        }else {
            logger.info("Order {} processing directly", orderId);
            result.put("order_status", "processing");
            result.put("message", String.format("订单 %s 正在处理中...", orderId));
        }

        result.put("processed_time", System.currentTimeMillis());

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        logger.info("OrderApprovalNode.interrupt() called, nodeId: {}", nodeId);

        // 1.从运行配置的元数据中，读取人工反馈信息（空安全Optional，不会空指针）
        Optional<Object> feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
        // 2. 判断：是否存在人工反馈（用户已经在human_feedback节点提交了内容）
        if (feedback.isPresent()) {
            Map<String, Object> feedbackData = (Map<String, Object>) feedback.get();
            // 从Map读取approved审批标记：true=人工同意，false=驳回
            Boolean approved = (Boolean) feedbackData.get("approved");
            logger.info("Human feedback received, approved: {}", approved);
            // 返回空Optional，代表当前分发无额外分支跳转标识
            return Optional.empty(); // 继续执行，调用 apply() 方法
        }

        Double orderAmount = state.value("order_amount", 0.0);
        String orderId = state.value("order_id", "UNKNOWN");
        if (orderAmount > APPROVAL_THRESHOLD) {
            logger.info("Order amount {} exceeds threshold {}, interrupting for approval", orderAmount, APPROVAL_THRESHOLD);

            InterruptionMetadata interruptionMetadata = InterruptionMetadata
                    .builder(nodeId, state)
                    .addMetadata("reason", "订单金额超过 " + APPROVAL_THRESHOLD + " 元，需要人工审批")
                    .addMetadata("order_id", orderId)
                    .addMetadata("order_amount", orderAmount)
                    .addMetadata("threshold", APPROVAL_THRESHOLD)
                    .addMetadata("interrupt_time", System.currentTimeMillis())
                    .build();
            return Optional.of(interruptionMetadata); // 中断执行，保存状态
        }

        logger.info("Order amount within limit, continuing");
        return Optional.empty();// 继续执行，调用 apply() 方法
    }


}


