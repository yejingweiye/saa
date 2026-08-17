package com.yjw.etl.hebing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.ContentFormatTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(RagPipelineService.class);

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final SimpleVectorStore vectorStore;

    public RagPipelineService(ChatModel chatModel, EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 1. 读取 PDF
     * 2. 文本分块
     * 3. 内容格式化
     * 4. 关键词/摘要增强
     * 5. 向量化入库
     */
    public List<Document> ingestPdf(String pdfPath) {
        logger.info("Start RAG ingest pipeline, pdfPath={}", pdfPath);

        // 1) 读取 PDF
        Resource resource = new DefaultResourceLoader().getResource(pdfPath);
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        List<Document> documents = reader.read();

        // 2) 文本分块
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> splitDocs = splitter.split(documents);

        // 3) 内容格式统一
        DefaultContentFormatter formatter = DefaultContentFormatter.defaultConfig();
        ContentFormatTransformer formatTransformer = new ContentFormatTransformer(formatter);
        List<Document> formattedDocs = formatTransformer.apply(splitDocs);

        // 4) 关键词增强
        KeywordMetadataEnricher keywordEnricher = new KeywordMetadataEnricher(this.chatModel, 3);
        List<Document> keywordDocs = keywordEnricher.apply(formattedDocs);

        // 5) 摘要增强
        List<SummaryMetadataEnricher.SummaryType> summaryTypes = List.of(
                SummaryMetadataEnricher.SummaryType.NEXT,
                SummaryMetadataEnricher.SummaryType.CURRENT,
                SummaryMetadataEnricher.SummaryType.PREVIOUS
        );
        SummaryMetadataEnricher summaryEnricher = new SummaryMetadataEnricher(this.chatModel, summaryTypes);
        List<Document> enrichedDocs = summaryEnricher.apply(keywordDocs);

        // 6) 向量化入库
        vectorStore.add(enrichedDocs);

        logger.info("RAG ingest finished, total docs={}", enrichedDocs.size());
        return enrichedDocs;
    }

    /**
     * 检索
     */
    public List<Document> search(String query, int topK) {
        logger.info("Search query={}", query);

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );
    }
}