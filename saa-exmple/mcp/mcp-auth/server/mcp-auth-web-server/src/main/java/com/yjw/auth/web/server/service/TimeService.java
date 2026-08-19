package com.yjw.auth.web.server.service;

import com.yjw.auth.web.server.util.UserInfoHolder;
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

    @Tool(description = "获取指定城市的当前时间。")
    public String getCityTimeMethod(@ToolParam(description = "时区 ID，例如 Asia/Shanghai") String timeZoneId) {

        // 从 UserInfoHolder 中获取当前用户信息
        String userInfo = UserInfoHolder.getUserInfo();
        // 工具业务可以根据登录信息获取用户数据
        // 例如 GitHub MCP 服务可以根据这个用户信息获取仓库列表等
        logger.info("当前时区是 {}，用户信息是 {}", timeZoneId, userInfo);

        return String.format("当前时区是 %s，当前时间是 %s", timeZoneId, getTimeByZoneId(timeZoneId));
    }

    private String getTimeByZoneId(String zoneId) {

        // 使用 ZoneId 获取时区
        ZoneId zid = ZoneId.of(zoneId);

        // 获取该时区下的当前时间
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zid);

        // 定义时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        // 将 ZonedDateTime 格式化成字符串
        String formattedDateTime = zonedDateTime.format(formatter);

        return formattedDateTime;
    }
}