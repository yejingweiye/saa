//package com.tianji.aigc.memory.mongodb;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.bson.types.ObjectId;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//import java.lang.annotation.Inherited;
//import java.util.List;
//
//@Data
//@Builder
//@AllArgsConstructor
//@NoArgsConstructor
//@Document("chat_record")
//public class ChatRecord {
//
//    @Id //标识为主键
//    private ObjectId id;
//
//    @Indexed
//    private String conversationId;
//
//    // 对话内容json字符串
//    private List<String> messages;
//
//
//}
