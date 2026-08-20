package com.yjw.mcp.node.tool;

import com.yjw.mcp.node.config.McpNodeProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MCP节点工具回调提供者
 * 根据nodeName，从全部MCP工具集合中筛选出当前节点绑定的MCP服务对应的ToolCallback
 */
@Service
public class McpClientToolCallbackProvider {

    private final ToolCallbackProvider toolCallbackProvider;

    private final McpClientCommonProperties commonProperties;

    private final McpNodeProperties mcpNodeProperties;


    /**
     * 构造注入MCP相关依赖
     * @param toolCallbackProvider SpringAI工具回调提供者，持有全部MCP注册的ToolCallback
     * @param commonProperties MCP客户端全局通用配置
     * @param mcpNodeProperties 自定义节点与MCP服务映射配置
     */
    // SpringBoot3.4+ 单构造函数可以省略@Autowired，写上更清晰
    public McpClientToolCallbackProvider(ToolCallbackProvider toolCallbackProvider,
                                         McpClientCommonProperties commonProperties,
                                         McpNodeProperties mcpNodeProperties) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.commonProperties = commonProperties;
        this.mcpNodeProperties = mcpNodeProperties;
    }

    /**
     * 根据节点名称，筛选该节点可用的MCP工具回调集合
     * @param nodeName graph节点名称
     * @return 当前节点匹配的ToolCallback集合，无匹配返回空集合，不会返回null
     */

    public Set<ToolCallback> findToolCallbacks(@Nullable String nodeName) {
        Set<ToolCallback> defineCallback = new HashSet<>();
        if (nodeName == null) {
            return defineCallback;
        }
        // 获取节点绑定的MCP服务名称集合
        Set<String> mcpClients = mcpNodeProperties.getNode2servers().get(nodeName);
        // 修复BUG：null或者空集合直接返回，原代码&&会NPE
        if (mcpClients == null || mcpClients.isEmpty()) {
            return defineCallback;
        }

        List<String> exceptMcpClientNames = new ArrayList<>();
        for (String mcpClient : mcpClients) {
            // my-mcp-client
            String name = commonProperties.getName();
            // 拼接工具前缀：{clientName}_{mcpServerName}
            // my_mcp_client_server1
            String prefixedMcpClientName = McpToolUtils.prefixedToolName(name.replace("-", "_"), mcpClient);
            exceptMcpClientNames.add(prefixedMcpClientName);
        }

        // 获取全部已注册MCP工具回调
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            // my_mcp_client_server1_getCityTimeMethod
            String toolFullName = toolDefinition.name();

            // 匹配前缀，筛选属于该MCP服务的工具
            for (String exceptMcpClientName : exceptMcpClientNames) {
                if (toolFullName.startsWith(exceptMcpClientName)) {
                    defineCallback.add(toolCallback);
                }
            }
        }
        return defineCallback;
    }
}