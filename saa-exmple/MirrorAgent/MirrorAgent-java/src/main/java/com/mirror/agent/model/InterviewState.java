package com.mirror.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 面试状态信息
 */
public class InterviewState {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("current_question")
    private int currentQuestion; // 当前问题编号

    @JsonProperty("total_questions")
    private int totalQuestions; // 总问题数

    @JsonProperty("current_difficulty")
    @Builder.Default // 默认值
    private String currentDifficulty = "medium"; // easy/medium/hard

    @JsonProperty("consecutive_right")
    private int consecutiveRight; // 连续答对数

    @JsonProperty("consecutive_wrong")
    private int consecutiveWrong; // 连续答错数

    @JsonProperty("qa_history")
    @Builder.Default
    private List<QAPair> qaHistory = new ArrayList<>(); // 问题回答历史

    @JsonProperty("candidate_profile")
    private String candidateProfile; // 候选人资料
}
