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
public class ResumeMatchResult {

    @JsonProperty("overall_score")
    private double overallScore; // 总分

    @JsonProperty("skill_match")
    private List<SkillMatch> skillMatch;

    private List<String> strengths; // 强项
    private List<String> weaknesses; // 弱项

    @JsonProperty("focus_areas")
    private List<String> focusAreas; // 面试重点考察方向

    @JsonProperty("resume_gaps")
    private List<String> resumeGaps; // 简历空白点（可深挖的地方）

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMatch {
        @JsonProperty("skill_name")
        private String skillName;

        private boolean required;
        private boolean matched;

        @JsonProperty("match_score")
        private double matchScore;

        private String evidence; // 从简历中找到的匹配证据
    }
}
