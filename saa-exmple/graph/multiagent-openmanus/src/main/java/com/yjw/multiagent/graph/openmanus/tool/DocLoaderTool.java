package com.yjw.multiagent.graph.openmanus.tool;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yjw.multiagent.graph.openmanus.tool.support.ToolExecuteResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocLoaderTool implements Function<String, ToolExecuteResult> {

    private static final Logger log = LoggerFactory.getLogger(DocLoaderTool.class);

    private static final String PARAMETERS = """
            {
                "type": "object",
                "properties": {
                    "file_type": {
                        "type": "string",
                        "description": "（必填）文件类型，例如 pdf、docx、xlsx、csv 等"
                    },
                    "file_path": {
                        "type": "string",
                        "description": "（必填）从用户请求中获取文件的绝对路径"
                    }
                },
                "required": ["file_type","file_path"]
            }
            """;

    private static final String name = "doc_loader";

    private static final String description = """
            获取指定路径下本地文件的内容信息。
            当你需要获取用户询问的相关文件信息时，请使用该工具。
            该工具接收文件路径，并读取返回对应的文件内容信息。
            """;


    public static OpenAiApi.FunctionTool getToolDefinition() {
        OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
        OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
        return functionTool;
    }

    public static FunctionToolCallback getFunctionToolCallback() {
        return FunctionToolCallback.builder(name, new BrowserUseTool())
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(String.class)
                .build();
    }

    public DocLoaderTool() {
    }

    public ToolExecuteResult run(String toolInput) {
        log.info("DocLoaderTool toolInput:{}", toolInput);
        try {
            Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
            });
            String fileType = (String) toolInputMap.get("file_type");
            String filePath = (String) toolInputMap.get("file_path");
            TikaDocumentParser parser = new TikaDocumentParser();
            List<Document> documentList = parser.parse(new FileInputStream(filePath));
            List<String> documentContents = documentList.stream()
                    .map(document -> document.getFormattedContent())
                    .collect(Collectors.toList());

            String documentContentStr = String.join("\n", documentContents);
            if (StringUtils.isEmpty(documentContentStr)) {
                return new ToolExecuteResult("No Related information");
            } else {
                return new ToolExecuteResult("Related information: " + documentContentStr);
            }
        } catch (Throwable e) {
            return new ToolExecuteResult("Error get Related information: " + e.getMessage());
        }
    }

    @Override
    public ToolExecuteResult apply(@ToolParam(description = PARAMETERS) String s) {
        return run(s);
    }

}
