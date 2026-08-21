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
