package com.yjw.vector.management.controller;

import com.alibaba.cloud.ai.request.EvidenceRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.SimpleVectorStoreManagementService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/simple")
public class SimpleVectorManagementController {

    @Autowired
    private SimpleVectorStoreManagementService simpleVectorStoreService;

    /*evidence的作用：
     ***数据库表 / 字段注释写不下、不方便写的业务规则、业务口径、字典含义**，全部放到 evidence。
     *这些内容会做 embedding 存入向量库；用户提问语义匹配到时，会被召回，拼进 Prompt 给大模型
     *
     *  1.业务规则：orders表status字段字典：0待支付，1已支付有效订单，2已取消，3退款。
     *统计销售额、有效订单数量时，只取 status =1 的数据。
     *
     *2.业务口径：月度销售额统计，只统计已支付订单；取消订单、退款订单不计入销售额。
     *
     */
    @PostMapping("/add/evidence")
    public Boolean addEvidence(@RequestBody List<EvidenceRequest> evidenceRequests) {
        return simpleVectorStoreService.addEvidence(evidenceRequests);
    }

    @PostMapping("/search")
    public List<Document> search(@RequestBody SearchRequest searchRequestDTO) throws Exception {
        return simpleVectorStoreService.search(searchRequestDTO);
    }

    @PostMapping("/delete")
    public Boolean deleteDocuments(@RequestBody com.alibaba.cloud.ai.request.DeleteRequest deleteRequest) throws Exception {
        return simpleVectorStoreService.deleteDocuments(deleteRequest);
    }


    @PostMapping("/init/schema")
    public Boolean schema(@RequestBody SchemaInitRequest schemaInitRequest) throws Exception {
        simpleVectorStoreService.schema(schemaInitRequest);
        return true;
    }

}
