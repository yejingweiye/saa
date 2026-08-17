package com.yjw.es.local;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.model.RerankModel;
import com.yjw.es.RagService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreProperties;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service()
public class LocalRagService implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(LocalRagService.class);

    private static final String textField = "content";

    private static final String vectorField = "embedding";

    @Value("classpath:/data/spring_ai_alibaba_quickstart.pdf")
    private Resource springAiResource;

    @Value("classpath:/prompts/system-qa.st")
    private Resource systemResource;

    // 注入

    private final ChatModel chatModel;

    private final VectorStore vectorStore;

    private final RerankModel rerankModel;

    private final ElasticsearchClient elasticsearchClient;

    private final ElasticsearchVectorStoreProperties options;

    public LocalRagService(ChatModel chatModel, VectorStore vectorStore, RerankModel rerankModel,
                           ElasticsearchClient elasticsearchClient, ElasticsearchVectorStoreProperties options) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.rerankModel = rerankModel;
        this.elasticsearchClient = elasticsearchClient;
        this.options = options;
    }


    @Override
    public void importDocuments() {
        // 1. parse document
        DocumentReader reader = new PagePdfDocumentReader(springAiResource);
        List<Document> documents = reader.get();
        logger.info("{} 文档已加载", documents.size());

        // 2. split trunks
        List<Document> splitDocuments = TokenTextSplitter.builder().build().apply(documents);
        logger.info("{} 文档已拆分", splitDocuments.size());

        // 3. create embedding and store to vector store
        logger.info("创建 embedding 并保存到向量存储");
        createIndexIfNotExists();
        vectorStore.add(splitDocuments);
    }


    @Override
    public Flux<ChatResponse> retrieve(String message) {
        // Enable hybrid search, both embedding and full text search
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(4)
                .similarityThresholdAll() // 不管分数高低，topK 查到多少就返回多少，哪怕相似度很低。
                .filterExpression(new FilterExpressionBuilder().eq(textField, message).build()) // 等于条件，textField == message
                .build();

        // Step3 - Retrieve and llm generate
        String promptTemplate = getPromptTemplate(systemResource);
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest, new SystemPromptTemplate(promptTemplate), 0.1))
                .build();

        return chatClient.prompt().user(message).stream().chatResponse();

    }

    // 创建索引如果不存在
    private void createIndexIfNotExists() {

        try {
            // 配置中获取
            String indexName = options.getIndexName();
            Integer dimsLength = options.getDimensions();

            if (StringUtils.isBlank(indexName)) {
                throw new IllegalArgumentException("必须提供 Elasticsearch 索引名称");
            }

            boolean exists = elasticsearchClient.indices().exists(idx -> idx.index(indexName)).value();

            if (exists) {
                logger.debug("Index {} already exists. Skipping creation.", indexName);
                return;
            }

            // 配置获取
            String similarityAlgo = options.getSimilarity().name();
            IndexSettings indexSettings = IndexSettings.of(
                    settings -> settings
                            .numberOfShards(String.valueOf(1))
                            .numberOfReplicas(String.valueOf(1))
            );

            // Maybe using json directly? 构建字段和额外信息
            Map<String, Property> properties = new HashMap<>();
            properties.put(vectorField, Property.of(property -> property.denseVector(
                    DenseVectorProperty.of(dense -> dense.index(true).dims(dimsLength).similarity(DenseVectorSimilarity.valueOf(similarityAlgo))))));
            properties.put(textField, Property.of(property -> property.text(TextProperty.of(t -> t))));

            Map<String, Property> metadata = new HashMap<>();
            metadata.put("ref_doc_id", Property.of(property -> property.keyword(KeywordProperty.of(k -> k))));

            properties.put("metadata",
                    Property.of(property -> property.object(ObjectProperty.of(op -> op.properties(metadata)))));

            CreateIndexResponse indexResponse = elasticsearchClient.indices()
                    .create(createIndexBuilder -> createIndexBuilder.index(indexName)
                            .settings(indexSettings)
                            .mappings(TypeMapping.of(mappings -> mappings.properties(properties))));


            if (!indexResponse.acknowledged()) {
                throw new RuntimeException("failed to create index");
            }

            logger.info("create elasticsearch index {} successfully", indexName);


        } catch (IOException e) {
            logger.error("failed to create index", e);
            throw new RuntimeException(e);
        }

    }

    private String getPromptTemplate(Resource systemResource) {
        try {
            return systemResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
