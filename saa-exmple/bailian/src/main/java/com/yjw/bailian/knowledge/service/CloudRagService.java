package com.yjw.bailian.knowledge.service;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 文档解析、切片、向量存储、向量检索全部跑在百炼云服务，本地 Java 服务只做调用。
 */
@Service()
public class CloudRagService implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(CloudRagService.class);

    private static final String indexName = "微服务";

    @Value("classpath:/data/spring_ai_alibaba_quickstart.pdf")
    private Resource springAiResource;

    private static final String retrievalSystemTemplate = """
			上下文信息如下。
			---------------------
			{question_answer_context}
			---------------------
			请仅依据上面给出的上下文以及对话历史信息进行回答，不要使用你已有的外部知识。
			回复用户的问题。如果上下文中找不到答案，请告知用户无法回答该问题。
			""";

    private final ChatClient chatClient;

    private final DashScopeApi dashscopeApi;

    public CloudRagService(ChatClient.Builder builder, DashScopeApi dashscopeApi) {
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
                DashScopeDocumentRetrieverOptions.builder().withIndexName(indexName).build());
        this.dashscopeApi = dashscopeApi;
        this.chatClient = builder
                .defaultAdvisors(new DocumentRetrievalAdvisor(retriever, new SystemPromptTemplate(retrievalSystemTemplate)))
                .build();
    }

        @Override
    public void importDocuments() {
            String path = saveToTempFile(springAiResource);

            // 1. 导入并切分文档
            DocumentReader reader = new DashScopeDocumentCloudReader(path, dashscopeApi, null);
            List<Document> documentList = reader.get();
            logger.info("{} documents loaded and split", documentList.size());

            // 1. 将文档添加到百炼云端向量存储
            VectorStore vectorStore = new DashScopeCloudStore(dashscopeApi, new DashScopeStoreOptions(indexName));
            vectorStore.add(documentList);
            logger.info("{} documents added to dashscope cloud vector store", documentList.size());
        }

    private String saveToTempFile(Resource springAiResource) {
        try {
            File tempFile = File.createTempFile("spring_ai_alibaba_quickstart", ".pdf");
            tempFile.deleteOnExit();

            try (InputStream inputStream = springAiResource.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile.getAbsolutePath();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Flux<ChatResponse> retrieve(String message) {
        return chatClient.prompt().user(message).stream().chatResponse();
    }
}
