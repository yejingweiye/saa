# 数据库对话接口模块

## 基于代码分析给你评估（没实测，只按改动和依赖关系判断）：

┌───────────────────────┬──────────────────────────┬──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│         接口          │        Controller        │                                                                       状态                                                                       │
├───────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ / 首页                │ HomeController           │ ✅ 能（纯静态页，启动日志已确认加载 index.html）                                                                                                 │
├───────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ /nl2sql/search 图链路 │ Nl2sqlForGraphController │ ✅ 能（已实测，返回 11,799.00 元报告）                                                                                                           │
├───────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ /simpleChat 简单      │ SimpleChatController     │ 🟡 应该能（走 SimpleVectorStoreService.schema() 全局库 + SimpleNl2SqlService.nl2sql()，不涉及 agentId，且 Spring AI 1.0.0                        │
│ NL2SQL                │                          │ 的版本修复对它同样生效；它只返回 SQL 文本，不执行）                                                                                              │
├───────────────────────┼──────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ /chat Analytic 链路   │ AnalyticNl2SqlController │ ❌ 不能（enabled 还是 false，且缺真实 AnalyticDB 云实例 + schema 初始化）                                                                        │
└───────────────────────┴──────────────────────────┴──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘



## 📌 项目简介

本模块是一个轻量级的数据库对话接口服务，基于 Spring Boot 构建。它对外暴露一个 `/chat` 接口，接收自然语言查询输入，并返回结构化的数据库问答结果。同时提供一个简单的可视化页面用于测试和展示对话功能。

---

## 🧩 核心功能

- **自然语言对话接口**
    - 提供 `/chat` 接口，支持通过自然语言查询数据库。
    - 输入为自然语言问题，输出为结构化字符串结果（如 SQL 查询或解释性文本）。

- **可视化交互页面**
    - 提供简洁的 HTML 页面，可直接在浏览器中进行对话测试。
    - 地址：[http://localhost:8065/index.html](http://localhost:8065/index.html)

---

## 🛠 技术栈

| 类别       | 技术/框架             |
|------------|------------------------|
| 后端语言   | Java 17+               |
| 框架       | Spring Boot            |
| 接口格式   | RESTful API (JSON)     |
| 前端界面   | HTML + CSS + JS        |
| 数据库支持 | MySQL / PostgreSQL     |

---

## 🚀 安装与部署

### ✅ 前置依赖

- [Java 17+](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
- [Maven](https://maven.apache.org/) 或 [Gradle](https://gradle.org/)
- 支持数据库连接（MySQL >= 5.7 / PostgreSQL >= 13）

### 🔧 配置修改

编辑配置文件 `application.yml`：

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode #类似 OpenAI 接口风格的兼容地址，这里指向的是阿里云 DashScope 的兼容接口。
      api-key: sk
      model: qwen-max #使用的模型名称，推荐使用：qwen-max: 适合复杂任务（如 NL2SQL）qwen-plus: 平衡性能与成本
    dashscope:
      api-key: sk  #DashScope 平台的 API Key，用于调用 Qwen 等模型。获取方式：登录 DashScope 控制台 → 查看或创建 API Key。
    vectorstore:
      analytic:
        enabled: true # 开启 AnalyticDB 向量数据库存储功能
        collectName: chatbi #向量集合名称，即你要写入数据的“collection”名，例如 chatbi
        regionId: cn-hangzhou #实例所在的区域 ID，比如 cn-hangzhou（杭州）、cn-beijing（北京）等。
        dbInstanceId: gp-bp11vjucxhw757v9p #AnalyticDB PostgreSQL 实例 ID，例如 gp-bp11vjucxhw757v9p
        managerAccount: #实例的管理员账号。
        managerAccountPassword: #实例的管理员密码。
        namespace: #命名空间信息，用于隔离不同用户的向量数据
        namespacePassword:
        defaultTopK: 10 #默认返回的相似向量数量。
        defaultSimilarityThreshold: 0.01 #通常设为 0.01 到 0.75 之间，根据实际效果调整。
        accessKeyId: #阿里云主账号或 RAM 用户的 AK 信息
        accessKeySecret:
chatbi:
  dbconfig:
    url: jdbc:mysql://host:port/database #数据库 JDBC 连接地址，示例：MySQL: jdbc:mysql://host:port/databasePostgreSQL: jdbc:postgresql://host:port/database
    username: #数据库用户名
    password: #数据库用户密码
    connectiontype: jdbc
    dialecttype: mysql #数据库类型，可选：postgresql、mysql
    schema: #postgresql类型所需要的schema名称
```

> 更多配置说明参考 : [Spring AI Alibaba NL2SQL文档](https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-nl2sql/spring-ai-alibaba-nl2sql-chat/README.md)。

### 💻 构建项目

使用 Maven 构建：

```bash
mvn clean install
```

或 Gradle：

```bash
gradle build
```

### ▶️ 启动服务

```bash
java -jar target/chat-1.0.0.jar
```

服务默认运行在 `http://localhost:8065`

---

## 🧪 使用说明

### 💬 对话接口 `/chat`

#### 请求方式：
- `POST`
- URL: `http://localhost:8065/chat`
- Content-Type: `application/json`

#### 请求示例：

```bash
curl --location 'http://localhost:8065/chat' \
--header 'Content-Type: application/json' \
--data '{"input": "我的菜鸟驿站里冬天收件数最多的是哪个菜鸟驿站"}'
```

#### 响应示例：

```json
"SELECT * FROM stations ORDER BY winter_package_count DESC LIMIT 1;"
```

---


## 查询每个分类下已成交且销量最高的商品

### 接口请求

- **请求方式**：POST
- **请求地址**：`http://localhost:8065/simpleChat`
- **请求头**：`Content-Type: application/json`
- **请求体示例**：

```
查询每个分类下已经成交且销量最高的商品及其销售总量，每个分类只返回销量最高的商品。
```

### SQL 查询说明

该查询会返回每个商品分类下，已成交（订单状态为已完成）的商品中销量最高的商品及其销售总量。每个分类只返回销量最高的那一件商品。

具体的表结构和测试数据请参考 `sql/schema.sql` 和 `sql/insert.sql` 文件。


## 🖼 可视化界面

访问以下地址即可打开可视化对话页面：

```
http://localhost:8065/index.html
```

该页面提供一个输入框和发送按钮，可实时查看对话结果。

---

## 📄 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源协议。

---



