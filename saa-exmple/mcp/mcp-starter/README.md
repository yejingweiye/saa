# MCP 客户端实现
mcp-sdk-streamable-client
1. 创建客户端传输Transport
2. 创建并初始化mcp客户端
3. 列出可用工具列表
4. 创建Client并注册mcp工具
5. 发送问题并获取回答

## stdio 通信
mcp-stdio-server vs mcp-stdio-client
最常用本地通信模式，用于同一台机器上，AI 客户端与 MCP 服务进程之间交换 JSON‑RPC 消息

## webflux 通信
mcp-webflux-server vs mcp-webflux-client
基于http和sse

## 流式通信
mcp-streamable-webflux-server vs mcp-streamable-webflux-client

##
mcp-webflux-server vs mcp-annotation-client 

## 多出
mcp-streamable-webmvc-server vs mcp-streamble-client 



