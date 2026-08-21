package com.yjw.vector.management.controller;

import com.alibaba.cloud.ai.request.DeleteRequest;
import com.alibaba.cloud.ai.request.EvidenceRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.service.AnalyticDbVectorStoreManagementService;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticDbVectorManagementController {

    @Autowired
    private AnalyticDbVectorStoreManagementService vectorStoreManagementService;

    @PostMapping("/add/evidence")
    public Boolean addEvidence(@RequestBody List<EvidenceRequest> evidenceRequests) {

        return vectorStoreManagementService.addEvidence(evidenceRequests);
    }

    @PostMapping("/search")
    public List<Document> search(@RequestBody SearchRequest searchRequestDTO) throws Exception{
        return vectorStoreManagementService.search(searchRequestDTO);
    }

    @PostMapping("/delete")
    public Boolean deleteDocuments(@RequestBody DeleteRequest deleteRequest) throws Exception{
        return vectorStoreManagementService.deleteDocuments(deleteRequest);
    }

    @PostMapping("/init/schema")
    public Boolean schema(@RequestBody SchemaInitRequest schemaInitRequest) throws  Exception{
        return vectorStoreManagementService.schema(schemaInitRequest);
    }

}
