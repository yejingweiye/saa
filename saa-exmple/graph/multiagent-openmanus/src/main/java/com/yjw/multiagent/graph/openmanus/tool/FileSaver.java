package com.yjw.multiagent.graph.openmanus.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yjw.multiagent.graph.openmanus.tool.support.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

public class FileSaver implements Function<String, ToolExecuteResult> {

    private static final Logger log = LoggerFactory.getLogger(FileSaver.class);

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
                        "description": "（必填）获取用户请求传入的文件绝对路径"
                    }
                },
                "required": ["file_type","file_path"]
            }
            """;

    private static final String name = "doc_loader";

    private static final String description = """
            读取指定路径的本地文件内容。
            用户提出和文件内容相关的查询需求时，调用此工具。
            工具接收文件路径参数，读取并返回文件对应的内容数据。
            """;

    public static OpenAiApi.FunctionTool getToolDefinition() {
        OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
        OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
        return functionTool;
    }

    public static FunctionToolCallback getFunctionToolCallback() {
        return FunctionToolCallback.builder(name, new FileSaver())
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(String.class)
                .build();
    }

    public ToolExecuteResult run(String toolInput) {
        log.info("FileSaver toolInput:{}", toolInput);
        try {
            Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
            });
            String content = (String) toolInputMap.get("content");
            String filePath = (String) toolInputMap.get("file_path");
            File file = new File(filePath);
            File directory = file.getParentFile();
            if (directory != null && !directory.exists()) {
                directory.mkdirs();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return new ToolExecuteResult("Content successfully saved to " + filePath);
        } catch (Throwable e) {
            return new ToolExecuteResult("Error saving file: " + e.getMessage());
        }
    }

    @Override
    public ToolExecuteResult apply(@ToolParam(description = PARAMETERS) String s) {
        return run(s);
    }

}
