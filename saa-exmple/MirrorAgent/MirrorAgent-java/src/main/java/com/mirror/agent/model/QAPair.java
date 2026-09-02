package com.mirror.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 面试问题对
 */
public class QAPair {

    private PlannedQuestion question;

    @JsonProperty("user_answer")
    private String userAnswer; // 用户答案

    private double score;
    private String feedback;

    @JsonProperty("follow_up_used")
    private boolean followUpUsed;


}
