package com.yjw.streamable.webflux.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class TimeService {

    private static final Logger logger = LoggerFactory.getLogger(TimeService.class);

    @Tool(description = "获取指定城市的时间。")
    public String getCityTimeMethod(@ToolParam(description = "时区ID，例如 Asia/Shanghai") String timeZoneId) {
        logger.info("当前传入时区为 {}", timeZoneId);
        return String.format("当前时区为 %s，当前时间为 %s", timeZoneId,
                getTimeByZoneId(timeZoneId));
    }

    private String getTimeByZoneId(String zoneId) {
        // 根据时区ID获取时区对象
        ZoneId zid = ZoneId.of(zoneId);

        // 获取该时区下的当前时间
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zid);

        // 定义时间格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        // 将带时区时间格式化为字符串
        String formattedDateTime = zonedDateTime.format(formatter);

        return formattedDateTime;
    }
}