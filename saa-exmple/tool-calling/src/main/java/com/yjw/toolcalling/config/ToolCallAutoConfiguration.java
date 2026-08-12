
package com.yjw.toolcalling.config;

import com.alibaba.cloud.ai.toolcalling.baidumap.BaiduMapSearchInfoService;
import com.alibaba.cloud.ai.toolcalling.time.GetTimeByZoneIdService;
import com.yjw.toolcalling.component.AddressInformationTools;
import com.yjw.toolcalling.component.CampusScheduleTools;
import com.yjw.toolcalling.component.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(GetTimeByZoneIdService.class)
public class ToolCallAutoConfiguration {

    @Bean
    public GetTimeByZoneIdService getCurrentTimeByTimeZoneIdService() {
        return new GetTimeByZoneIdService();
    }

    @Bean
    public TimeTools timeTools(GetTimeByZoneIdService service) {
        return new TimeTools(service);
    }

    @Bean
    public CampusScheduleTools campusScheduleTools() {
        return new CampusScheduleTools();
    }

    @Bean
    public AddressInformationTools addressInformationTools(BaiduMapSearchInfoService service) {
        return new AddressInformationTools(service);
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

}
