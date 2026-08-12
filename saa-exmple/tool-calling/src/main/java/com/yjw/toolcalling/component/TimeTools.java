package com.yjw.toolcalling.component;

import com.alibaba.cloud.ai.toolcalling.time.GetTimeByZoneIdService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class TimeTools {

    private final GetTimeByZoneIdService timeService;

    public TimeTools(GetTimeByZoneIdService timeService) {
        this.timeService = timeService;
    }

    @Tool(description = "获取指定城市的时间。")
    public String getCityTime(@ToolParam(description = "时区ID，例如 Asia/Shanghai")
                              String timeZoneId) {

        return timeService.apply(new GetTimeByZoneIdService.Request(timeZoneId)).description();
    }

}