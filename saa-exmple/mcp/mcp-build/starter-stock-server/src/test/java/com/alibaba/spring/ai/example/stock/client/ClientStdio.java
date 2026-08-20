package com.alibaba.spring.ai.example.stock.client;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * MCP Stdio模式客户端示例
 * 通过标准输入输出启动本地MCP服务端Jar，调用股票查询工具进行测试
 */
public class ClientStdio {

    public static void main(String[] args) {
        // 构建服务端启动参数：使用java命令启动MCP服务端jar包
        var stdioParams = ServerParameters.builder("java")
                .args("-Dspring.ai.mcp.server.stdio=true",
                        "-Dspring.main.web-application-type=none",
                        "-Dlogging.pattern.console=",
                        "-jar",
                        "saa-exmple/mcp/mcp-build/starter-stock-server/target/starter-stock-server-1.0.0.jar")
                .build();

        // 创建Stdio传输通道，MCP服务端通过子进程标准输入输出通信
        var transport = new StdioClientTransport(stdioParams, McpJsonMapper.getDefault());
        // 构建同步模式MCP客户端
        var client = McpClient.sync(transport).build();

        try {
            // 初始化MCP连接，完成客户端与服务端握手
            client.initialize();

            // 获取并打印服务端提供的全部可用工具列表
            ListToolsResult toolsList = client.listTools();
            System.out.println("可用工具列表 = " + toolsList);

            // 测试沪市股票（600519）
            System.out.println("\n===== 测试沪市股票(600519) =====");
            CallToolResult shStockResult = client.callTool(new CallToolRequest("getStockInfo",
                    Map.of("stockCode", "600519")));
            System.out.println("股票行情返回结果: " + shStockResult);

            // 测试深市股票（000001）
            System.out.println("\n===== 测试深市股票(000001) =====");
            CallToolResult szStockResult = client.callTool(new CallToolRequest("getStockInfo",
                    Map.of("stockCode", "000001")));
            System.out.println("股票行情返回结果: " + szStockResult);

            // 测试不存在的股票代码（999999）
            System.out.println("\n===== 测试不存在的股票(999999) =====");
            try {
                CallToolResult invalidStockResult = client.callTool(new CallToolRequest("getStockInfo",
                        Map.of("stockCode", "999999")));
                System.out.println("股票行情返回结果: " + invalidStockResult);
            } catch (Exception e) {
                System.out.println("捕获预期异常: " + e.getMessage());
            }

            // 测试非法格式股票代码（abc非数字）
            System.out.println("\n===== 测试非法格式股票代码(abc) =====");
            try {
                CallToolResult invalidCodeResult = client.callTool(new CallToolRequest("getStockInfo",
                        Map.of("stockCode", "abc")));
                System.out.println("股票行情返回结果: " + invalidCodeResult);
            } catch (Exception e) {
                System.out.println("捕获预期异常: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("MCP测试过程发生异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 优雅关闭MCP客户端，终止子进程，释放资源
            client.closeGracefully();
        }
    }
}