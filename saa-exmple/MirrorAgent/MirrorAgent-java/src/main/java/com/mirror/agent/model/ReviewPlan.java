package com.mirror.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPlan {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("weak_areas")
    private List<WeakArea> weakAreas; // 弱点列表

    @JsonProperty("study_plan")
    private List<StudyItem> studyPlan; // 学习计划

    private List<Resource> resources; // 资源列表

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakArea {
        private String topic; // 弱点主题
        private double score;
        private String priority;  // high/medium/low
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyItem {
        private String topic; // 学习主题
        private String objective; // 学习目标
        private List<String> actions; // 学习动作

        @JsonProperty("time_estimate")
        private String timeEstimate; // 时间估计
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private String title;
        private String type;  // article/video/repo/book
        private String url;
        private String desc;
    }


}
