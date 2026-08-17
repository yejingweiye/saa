package com.yjw.modulerag.init;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.stylesheets.LinkStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VectorDBInit {

    private static final Logger logger = LoggerFactory.getLogger(VectorDBInit.class);

    private final VectorStore vectorStore;

    @Value("classpath:documents/story-1.md")
    Resource file1;

    @Value("classpath:documents/story-2.md")
    Resource file2;

    private final ElasticsearchClient elasticsearchClient;

    private final ElasticsearchVectorStoreProperties options;

    private static final String textField = "content";

    private static final String vectorField = "embedding";

    public VectorDBInit(
            VectorStore vectorStore,
            ElasticsearchClient elasticsearchClient,
            ElasticsearchVectorStoreProperties options) {

        this.vectorStore = vectorStore;
        this.elasticsearchClient = elasticsearchClient;
        this.options = options;
    }

    @PostConstruct
    void run() {
        logger.info("加载 *.md 文件为文档");

        var markdownReader1 = new MarkdownDocumentReader(file1, MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("location", "北极")
                .build());

        List<Document> documents = new ArrayList<>(markdownReader1.read());


        var markdownReader2 = new MarkdownDocumentReader(file2, MarkdownDocumentReaderConfig.builder()
                .withAdditionalMetadata("location", "意大利")
                .build());
        documents.addAll(markdownReader2.get());

        logger.info("从文档创建并存储嵌入");

        createIndexIfNotExists();
        vectorStore.add(new TokenTextSplitter().split(documents));

        // ======================直接查询校验======================
        SearchRequest verifyReq = SearchRequest.builder()
                .query("约雷克")
                .topK(5)
                .build();
        List<Document> resultDocs = vectorStore.similaritySearch(verifyReq);
        logger.info("不带过滤查询全部，命中数量：{}", resultDocs.size());
        for (Document doc : resultDocs) {
            logger.info("doc metadata:{}", doc.getMetadata());
        }


        logger.info("✅校验通过，向量数据已成功落库");

    }

    private void createIndexIfNotExists() {
        try {
            String indexName = options.getIndexName();
            Integer dimsLength = options.getDimensions();

                if (!StringUtils.hasLength(indexName)) {
                throw new IllegalArgumentException("必须提供 Elasticsearch 索引名称");
            }

            boolean exists = elasticsearchClient.indices().exists(idx -> idx.index(indexName)).value();
            if (exists) {
                logger.debug("索引 {} 已存在，跳过创建。", indexName);
                return;
            }

            String similarityAlgo = options.getSimilarity().name();
            IndexSettings indexSettings = IndexSettings
                    .of(settings -> settings.numberOfShards(String.valueOf(1)).numberOfReplicas(String.valueOf(1)));

            // Maybe using json directly?
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
                throw new RuntimeException("创建索引失败");
            }

            logger.info("成功创建 Elasticsearch 索引 {}", indexName);
        }
        catch (IOException e) {
            logger.error("创建索引失败", e);
            throw new RuntimeException(e);
        }
    }
}
