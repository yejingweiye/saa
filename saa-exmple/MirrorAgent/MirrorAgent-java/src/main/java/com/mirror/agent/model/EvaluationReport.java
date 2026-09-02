package com.mirror.agent.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationReport {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("candidate_name")
    private String candidateName; // 候选人姓名

    private String position; // 职位名称

    @JsonProperty("overall_score")
    private double overallScore; // 总分

    @JsonProperty("overall_level")
    private String overallLevel;    // A/B/C/D

    @JsonProperty("dimension_score")
    private Map<String, Double> dimensionScore; // 各维度分

    private List<String> strengths; // 候选人优势
    private List<String> weaknesses; // 候选人劣势

    @JsonProperty("detailed_review")
    private List<QuestionReview> detailedReview; // 问题详细 review

    private String summary; // 总结

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionReview {
        @JsonProperty("question_content")
        private String questionContent; // 问题内容

        @JsonProperty("user_answer")
        private String userAnswer; // 用户答案

        private double score; // 分数
        private String comment; // 评论

        @JsonProperty("key_points_hit")
        private List<String> keyPointsHit; // 击中点

        @JsonProperty("key_points_missed")
        private List<String> keyPointsMissed; // 未击中点
    }

}
