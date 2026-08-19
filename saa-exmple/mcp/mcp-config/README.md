
## 也就是优先级：文件配置 > 数据库配置 > Nacos 配置
### 从数据库中读取

在 application.yml 中添加数据库（如 MySQL）配置，举例：

```yml
spring.ai.alibaba.mcp.router:
   enabled: true  # 启用MCP路由
   database:
      enabled: true
      url: jdbc:mysql://localhost:3306/testdb?useSSL=false&serverTimezone=UTC
      username: root
      password: root
      driverClassName: com.mysql.cj.jdbc.Driver
      tableName: mcp_server_info
```

创建 MySQL 数据库表并添加示例记录，举例：

```sql
-- 创建mcp_server_info表
CREATE TABLE IF NOT EXISTS mcp_server_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE COMMENT '服务名称',
    description TEXT COMMENT '服务描述',
    protocol VARCHAR(50) COMMENT '服务协议',
    version VARCHAR(50) COMMENT '服务版本',
    endpoint VARCHAR(255) COMMENT '服务访问端点',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    tags VARCHAR(255) COMMENT '标签，逗号分隔',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 添加示例记录
INSERT INTO mcp_server_info (name, description, protocol, version, endpoint, enabled, tags)
VALUES
('dashscope-chat', '阿里云通义千问大模型服务', 'http', 'v1', 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/num-tokens', TRUE, 'chat,llm,aliyun'),
('openai-embedding', 'OpenAI Embedding服务', 'http', 'v1', 'https://api.openai.com/v1/embeddings', TRUE, 'embedding,openai'),
('custom-service-a', '自定义服务A', 'grpc', 'v1.0', 'grpc://localhost:9090', TRUE, 'custom,test');
```

发送 HTTP GET 请求，从 MySQL 数据库读取 MCP 服务配置信息。

### 从 Nacos 配置中心读取

在 application.yml 中添加 Nacos 配置，举例：

```yml
spring.ai.alibaba.mcp.nacos:
  server-addr: localhost:8848
  namespace: public
```