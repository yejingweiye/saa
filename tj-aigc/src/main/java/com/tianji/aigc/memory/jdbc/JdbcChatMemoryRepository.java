package com.tianji.aigc.memory.jdbc;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.aigc.entity.ChatRecord;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.service.ChatRecordService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class JdbcChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private ChatRecordService chatRecordService;

    @Override
    public List<String> findConversationIds() {
        var chatRecordList = this.chatRecordService.lambdaQuery()
                .select(ChatRecord::getConversationId)
                .list();
        return CollStreamUtil.toList(chatRecordList, ChatRecord::getConversationId);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        var chatRecordList = this.chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByAsc(ChatRecord::getCreateTime)
                .list();
        return CollStreamUtil.toList(chatRecordList, chatRecord -> MessageUtil.toMessage(chatRecord.getData()));
    }


    @Override
    public void saveAll(String conversationId, List<Message> messages) {

        // 先删除数据
        this.deleteByConversationId(conversationId);
        var userId = Convert.toLong(StrUtil.subBefore(conversationId, "_", true)) ;

        // 保存数据
        var chatRecordList =CollStreamUtil.toList(messages, message -> ChatRecord.builder()
                .conversationId(conversationId)
                .data(MessageUtil.toJson(message))
                .creater(userId)
                .updater(userId)
                .build());
        this.chatRecordService.saveBatch(chatRecordList);


    }

    @Override
    public void deleteByConversationId(String conversationId) {
        var queryWrapper = Wrappers.<ChatRecord>lambdaQuery().
                eq(ChatRecord::getConversationId, conversationId);
        this.chatRecordService.remove(queryWrapper);
    }

}
