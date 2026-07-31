//package com.tianji.aigc.memory.mongodb;
//
//import cn.hutool.core.collection.CollStreamUtil;
//import cn.hutool.core.convert.Convert;
//import cn.hutool.core.util.StrUtil;
//import com.baomidou.mybatisplus.core.toolkit.Wrappers;
//import com.tianji.aigc.memory.MessageUtil;
//import jakarta.annotation.Resource;
//import org.springframework.ai.chat.memory.ChatMemoryRepository;
//import org.springframework.ai.chat.messages.Message;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//
//import java.util.List;
//
//public class MongoDBChatMemoryRepository implements ChatMemoryRepository {
//
//    @Resource
//    private MongoTemplate mongoTemplate;
//
//    @Override
//    public List<String> findConversationIds() {
//        var chatRecordList = this.mongoTemplate.findAll(ChatRecord.class); //查询全部出来可能内存溢出
//        return CollStreamUtil.toList(chatRecordList, ChatRecord::getConversationId);
//    }
//
//    @Override
//    public List<Message> findByConversationId(String conversationId) {
//        var query = Query.query(Criteria.where("conversationId").is(conversationId));
//        var chatRecord = this.mongoTemplate.findOne(query, ChatRecord.class);
//        if (null == chatRecord) {
//            return List.of();
//        }
//        return CollStreamUtil.toList(chatRecord.getMessages(), MessageUtil::toMessage);
//    }
//
//
//    @Override
//    public void saveAll(String conversationId, List<Message> messages) {
//
//        // 先删除数据
//        this.deleteByConversationId(conversationId);
//        // 保存数据
//        var chatRecord = ChatRecord.builder()
//                .conversationId(conversationId)
//                .messages(CollStreamUtil.toList(messages, MessageUtil::toJson))
//                .build();
//        this.mongoTemplate.save(chatRecord);
//
//
//    }
//
//    @Override
//    public void deleteByConversationId(String conversationId) {
//        var query = Query.query(Criteria.where("conversationId").is(conversationId));
//        this.mongoTemplate.remove(query, ChatRecord.class);
//    }
//
//}
