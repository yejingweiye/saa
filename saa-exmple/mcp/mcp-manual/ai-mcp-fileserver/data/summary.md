# Spring AI MCP SDK 概述总结

## 核心定位
- **协议实现**：MCP（Model Context Protocol）协议的Java语言SDK实现。
- **通信能力**：提供同步与异步客户端，用于与MCP服务端进行标准化交互。

## 主要功能
- **客户端支持**：
  - 同步客户端（阻塞式调用）
  - 异步客户端（非阻塞式调用，基于Project Reactor）
- **标准操作支持**：
  - 工具发现（Tool Discovery）与工具执行（Tool Execution）
  - 资源管理（Resource Management）与资源模板（Resource Templates）
  - 提示词处理与管理（Prompt Handling & Management）
  - 资源订阅（Resource Subscription）
  - 服务初始化与心跳检测（Ping）
- **传输方式**：支持基于标准输入输出（Stdio）的服务通信。

## 技术特点
- 遵循MCP协议规范，确保跨平台、跨语言兼容性。
- 响应式编程支持，提升高并发场景下的系统性能与可扩展性。