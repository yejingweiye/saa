package com.mirror.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPlan {

    @JsonProperty("total_questions")
    private int totalQuestions; // 总问题数

    private QuestionDistrib distribution; // 问题分布

    private List<PlannedQuestion> questions; // 计划问题

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDistrib {
        private int basic;
        private int experience;
        private int design;
    }

}
