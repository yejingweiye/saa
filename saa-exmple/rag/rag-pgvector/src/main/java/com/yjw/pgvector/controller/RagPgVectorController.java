package com.yjw.pgvector.controller;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.model.RerankModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
public class RagPgVectorController {

    @Value("classpath:/prompts/system-qa.st")
    private Resource systemResource;

    @Value("classpath:/data/spring_ai_alibaba_quickstart.pdf")
    private Resource springAiResource;

    private final VectorStore vectorStore;

    private final ChatModel chatModel;

    // 重排序模型
    private final RerankModel rerankModel;

    public RagPgVectorController(VectorStore vectorStore, ChatModel chatModel, RerankModel rerankModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.rerankModel = rerankModel;
    }

    @GetMapping("/rag/importDocument")
    public void importDocument() {
        // 解析document
        DocumentReader reader = new PagePdfDocumentReader(springAiResource);
        List<Document> documents = reader.get();

        // 1.2 使用本地文件
        // FileSystemResource fileSystemResource = new FileSystemResource("D:\\file.pdf");
        // DocumentReader reader = new PagePdfDocumentReader(fileSystemResource);

        // 2. 拆分文档块
        List<Document> splitDocuments = new TokenTextSplitter().apply(documents);

        // 3. 创建向量并存储到向量数据库
        vectorStore.add(splitDocuments);

    }

    /**
     * 接收任意长度的文本，进行拆分并写入向量存储
     */
    @GetMapping("/rag/importText")
    public ResponseEntity<String> insertText(@RequestParam("text") String text) {
        // 1. 参数校验
        if (!StringUtils.hasText(text)) {
            return ResponseEntity.badRequest().body("Please enter text");
        }

        // 2. 将文本封装为 Document
        List<Document> documents = List.of(new Document(text));

        // 3. 拆分文本为多个片段
        List<Document> splitDocuments = new TokenTextSplitter().apply(documents);

        // 4. 为片段创建向量并写入向量存储
        vectorStore.add(splitDocuments);
        // 5. 返回成功提示
        String msg = String.format("成功将 %d 段文本插入到向量存储中", splitDocuments.size());
        return ResponseEntity.ok(msg);
    }

    /**
     * 读取并解析上传的文件，将文件内容拆分并写入向量存储
     *
     * @param file 上传的文件（MultipartFile），支持单个文件上传
     * @return 处理结果的 ResponseEntity，成功返回提示信息，失败返回 400 错误
     */
    @PostMapping(value = "/rag/importFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> insertFiles(@RequestPart(value = "file", required = false) MultipartFile file) {
        // 1. 文件校验
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("必须上传非空的文件");
        }

        // 2. 解析文件（例如根据文件类型选择不同的 DocumentReader）
        List<Document> documents = new TikaDocumentReader(file.getResource()).get();

        // 3. 将解析得到的文本进行拆分（分片）
        List<Document> splitDocs = new TokenTextSplitter().apply(documents);

        // 4. 为每个片段创建向量并写入向量存储
        vectorStore.add(splitDocs);
        // 5. 返回处理结果提示
        String msg = String.format("成功将 %d 段文本插入到向量存储中", splitDocs.size());
        return ResponseEntity.ok(msg);
    }

    // 用户消息 → RetrievalRerankAdvisor执行向量检索+重排 → 拼接上下文到SystemPrompt → LLM生成流式输出
    @GetMapping(value = "/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generate(@RequestParam(value = "message",
            defaultValue = "how to get start with spring ai alibaba?") String message) throws IOException {
        SearchRequest searchRequest = SearchRequest.builder().topK(2).build();

        String promptTemplate = systemResource.getContentAsString(StandardCharsets.UTF_8);

        return ChatClient.builder(chatModel)
                .defaultAdvisors(new RetrievalRerankAdvisor(
                        vectorStore,
                        rerankModel,
                        searchRequest,
                        new SystemPromptTemplate(promptTemplate), // 使用模版
                        0.1
                ))
                .build()
                .prompt()
                .user(message)
                .stream()
                .chatResponse();

    }

    /**
     * 读取并解析文件，写入向量数据库
     *
     * @param file 上传的文件
     * @return 返回导入结果信息
     */
    @PostMapping(value = "/rag/importFileV2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importFileV2(@RequestPart(value = "file", required = false) MultipartFile file) {

        // 1. 文件校验
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("必须上传非空的文件");
        }

        // 2. 解析文件内容
        List<Document> documents = new TikaDocumentReader(file.getResource()).get();

        // 3. 文本切块
        List<Document> splitDocs = new TokenTextSplitter().apply(documents);
        String fileId = UUID.randomUUID().toString();
        for (Document doc : splitDocs){
            doc.getMetadata().put("fileId",fileId); // 为当前文件所有切块文档设置同一个文件ID
        }

        // 4. 生成向量并保存到向量库
        vectorStore.add(splitDocs);

        // 5. 返回成功提示
        String msg = String.format("成功向向量库插入 %d 个文本分片，fileId: %s", splitDocs.size(), fileId);
        return ResponseEntity.ok(msg);
    }

    /**
     * search the vector store
     * 从指定的fileId中找到相关的文档，并返回结果
     * @param message
     * @param fileId
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/rag/searchV2")
    public  Flux<String> search(@RequestParam(value = "message",
                                        defaultValue = "what is blibaba?") String message,
                                @RequestParam(value = "fileId", required = true)
                                String fileId) throws IOException {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.eq("fileId", fileId).build();

        //
        SearchRequest searchRequest = SearchRequest.builder().topK(1).filterExpression(expression).build();
        String promptTemplate = systemResource.getContentAsString(StandardCharsets.UTF_8);

        return ChatClient.builder(chatModel)
                .defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest, new SystemPromptTemplate(promptTemplate), 0.1))
                .build()
                .prompt()
                .user(message)
                .stream()
                .content();

    }

    @PostMapping(value = "/rag/deleteFilesV2")
    public ResponseEntity<String> deleteFiles(@RequestParam(value = "fileId", required = false) String fileId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.eq("fileId", fileId).build();
        vectorStore.delete(expression);
        return ResponseEntity.ok("successfully deleted");
    }




    }
