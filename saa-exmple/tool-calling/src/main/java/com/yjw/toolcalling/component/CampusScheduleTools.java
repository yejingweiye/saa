package com.yjw.toolcalling.component;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class CampusScheduleTools {

    @Tool(description = "根据用户请求，生成简洁的校园活动日程安排。")
    public String createCampusSchedule(
            @ToolParam(description = "校园活动或者学习目标。") String activity,
            @ToolParam(description = "建议开始时间，例如：14:00。") String startTime,
            @ToolParam(description = "持续时长，单位：分钟。") int durationMinutes) {

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("持续时长必须大于0。");
        }

        return "校园日程：活动=" + activity + "，开始时间=" + startTime
                + "，持续时长(分钟)=" + durationMinutes + "。";
    }

}
