# sc-gateway 网关模块

## 模块概述

基于 **Spring Cloud Gateway**（WebFlux 响应式，非 SpringMVC）的统一入口网关服务。服务名 `gateway-service`，端口 `10010`。负责所有微服务请求的路由转发、JWT 登录鉴权、接口权限校验、请求链路标识注入以及全局异常兜底。

## 技术栈

| 组件 | 说明 |
|---|---|
| spring-cloud-starter-gateway | 网关框架（WebFlux，异常处理需用 `ErrorWebExceptionHandler`，不能用 `@ControllerAdvice`） |
| sc-auth-gateway-sdk | JWT 验签（公钥来自 auth-service 的 `/jwks`）+ Redis 接口权限校验 |
| nacos-discovery / nacos-config | 注册中心 / 配置中心 |
| spring-cloud-starter-loadbalancer | 服务发现后的负载均衡 |

## 核心功能

### 1. 路由转发

`application.yml` 通过 `spring.cloud.gateway.routes` 定义 15 条路由（`/ms/**`、`/as/**`、`/us/**`、`/ais/**` 等），统一转发到 `lb://服务名`，并配置 `default-filters: StripPrefix=1` 去除路由前缀后转发到下游服务。

例：`/as/accounts/login` → auth-service 的 `/accounts/login`。

### 2. 请求链路标识注入 —— RequestIdRelayFilter（最先执行）

为每个请求生成链路 ID 并注入下游请求头，用于全链路日志追踪：

- 生成 UUID 写入日志变量池 MDC（`requestId`）
- 注入 `requestId` 请求头
- 注入 `x-request-from: gateway` 来源标识（路径以 `/ps/notify` 开头的支付回调**不注入**，以便下游区分外部回调与网关转发）

### 3. 登录鉴权 + 接口权限校验 —— AccountAuthFilter（order = 1000）

核心流程：

1. 组装 ant 匹配路径 `METHOD:path`（如 `GET:/as/menus/me`）
2. 命中 `tj.auth.exclude-path` 白名单则直接放行（代码中默认追加 `/error/**`、`/jwks`、`/accounts/login`、`/accounts/admin/login`、`/accounts/refresh`）
3. 从 `authorization` 头取 token，`AuthUtil.parseToken` 用 RSA 公钥验签 + 校验有效期 + 解析 `user` payload → `LoginUserDTO`（userId / roleId / rememberMe）
4. token 有效时，将用户信息注入下游请求头（见"传递给下游的数据"）
5. `AuthUtil.checkAuth` 做接口权限校验：权限表由 auth-service 写入 Redis（hash `auth:privileges`，antPath → 所需角色集合，版本号 `version`），网关每 20s 定时刷新到本地内存；若请求路径受权限保护而用户 `roleId` 不在允许角色集合 → 403
6. 未登录访问受保护接口 → 401

### 4. 全局异常处理 —— GatewayExceptionHandler

实现 WebFlux 的 `ErrorWebExceptionHandler`（最高优先级），统一捕获网关异常并输出 `R<T>` JSON（含 requestId）：

- `UnauthorizedException` → 直接返回对应 HTTP 状态码
- `CommonException` → 返回其 code / msg
- `NotFoundException` → "服务不存在"
- `ResponseStatusException` → 透传 message
- 其他未知异常 → "服务器内部错误" + 打印堆栈

### 5. 全局跨域

`globalcors` 放开所有来源 / 方法，允许携带 Cookie，预检（OPTIONS）请求直接放行。

### 6. Swagger 聚合（已停用）

`GatewaySwaggerResourceProvider`、`SwaggerResourceController` 均已整体注释，当前不生效。

## 传递给下游服务的数据（请求头）

| Header | 注入位置 | 说明 |
|---|---|---|
| `user-info` | AccountAuthFilter（仅 token 有效时） | 用户 ID（Long → String）。下游 `sc-auth-resource-sdk` 的 `UserInfoInterceptor` 读取并写入 `UserContext` |
| `token-info` | AccountAuthFilter（仅 token 有效时） | 原始 JWT token，下游可二次校验 / 续期 |
| `requestId` | RequestIdRelayFilter | 链路追踪 ID，与日志 MDC 对应 |
| `x-request-from` | RequestIdRelayFilter（`/ps/notify` 除外） | 固定值 `gateway`，下游可区分请求来源（gateway / feign / 外部） |

> 说明：网关鉴权通过后把登录态以 `user-info` 下发给下游，下游依赖 `sc-auth-resource-sdk` 的 `UserInfoInterceptor` 解析出 userId、`LoginAuthInterceptor` 判断登录状态。网关鉴权与下游拦截为配套机制。

## 关键配置

- `server.port: 10010`
- `spring.cloud.gateway.routes`：路由表（Path 前缀 → `lb://服务`）
- `spring.cloud.gateway.default-filters: StripPrefix=1`
- `tj.auth.exclude-path`：网关层无需登录校验的路径（来自 nacos 配置 `gateway-service.yaml`，并在 `AuthProperties.afterPropertiesSet` 中追加上述 5 个默认路径）

## 过滤器执行顺序

| 优先级 | 过滤器 | 作用 |
|---|---|---|
| 最小（最先） | RequestIdRelayFilter | 注入 `requestId` / `x-request-from` |
| 1000 | AccountAuthFilter | 登录鉴权 + 接口权限校验 + 注入 `user-info` / `token-info` |
