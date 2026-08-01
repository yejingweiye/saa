# sc-auth-gateway-sdk

网关鉴权 SDK，为 Spring Cloud 微服务网关提供 **JWT 令牌校验** 与 **接口权限校验** 能力。

网关引入该 SDK 后，即可自动完成：
1. 从注册中心发现 `auth-service`，拉取 RSA 公钥用于校验 JWT 签名；
2. 解析 token 得到登录用户信息（`LoginUserDTO`）；
3. 根据接口 ant 路径判断当前用户是否有访问权限。

## 工作原理

```
网关请求进入
    │
    ▼
parseToken(token) ──► 使用 JwtSignerHolder 的公钥校验签名/有效期 ──► LoginUserDTO
    │                                                        │
    ▼                                                        ▼
checkAuth(antPath, r) ◄──── 从 Redis 加载的接口权限缓存(antPath → 角色集合)
    │                                                        │
    ▼                                                        │
放行 / 抛 UnauthorizedException / 抛 ForbiddenException ◄────┘
```

依赖关系：`AuthUtil`（鉴权逻辑）依赖 `JwtSignerHolder`（公钥/JWT 验签器），两者均由 `AuthAutoConfiguration` 自动注册为 Spring Bean。

## 模块组成

| 类 | 类型 | 作用 |
|---|---|---|
| `JwtSignerHolder` | 工具类（Bean） | RSA 公钥加载器，维护 JWT 验签器 |
| `AuthUtil` | 工具类（Bean） | token 解析 + 接口权限校验 |
| `AuthAutoConfiguration` | 自动配置类 | 注册上述两个 Bean，通过 `META-INF/spring` SPI 被 Spring Boot 自动装配 |

依赖模块：`sc-auth-common`（常量与 DTO）、`spring-boot-starter-data-redis`（权限缓存）、`spring-cloud-commons`（服务发现）。

---

## 工具一：JwtSignerHolder — JWT 验签器持有者

### 作用

网关侧的 **RSA 公钥加载器**。JWT 采用 **RS256** 非对称签名（私钥在 `auth-service`，公钥在网关侧），本类负责自动获取公钥并缓存 `JWTSigner`，供 `AuthUtil.parseToken` 校验 token 签名。

### 功能逻辑

1. **异步加载**：`@PostConstruct` 时向单线程池（线程名 `AuthFetchJwkThread`）提交 `JwkTask`，不阻塞网关启动。
2. **服务发现**：通过 `DiscoveryClient` 从注册中心获取 `auth-service` 服务实例（取第一个）。
3. **拉取公钥**：调用 `http://{host}:{port}/jwks` 接口，拿到 Base64 编码的 RSA 公钥。
4. **构建验签器**：用 `KeyUtil.generatePublicKey(RSA, ...)` 解析出 `PublicKey`，再通过 `JWTSignerUtil.createSigner("rs256", publicKey)` 创建 `JWTSigner`，赋值给 volatile 字段 `jwtSigner` 全局缓存。
5. **失败循环重试**：实例列表为空、请求失败或数据为空时，打印错误日志并 `sleep(10s)` 后重试，直到成功为止。
6. **自动回收**：成功后调用 `shutdown()` 关闭线程池，避免资源泄漏。

### 关键字段

| 字段 | 说明 |
|---|---|
| `jwtSigner` | 缓存的 JWT 验签器（volatile 保证多线程可见性），成功前为 `null` |
| `discoveryClient` | 服务发现客户端，用于定位 `auth-service` |
| `ses` | 单线程线程池（核心/最大线程数均为 1，队列容量 1） |

### 作用价值

- 网关与认证服务解耦：公钥由 `auth-service` 统一维护、动态下发，网关无需硬编码或本地存放公钥文件。
- 公钥更新或 `auth-service` 重启后，网关可通过该机制重新拉取，提高可用性。

---

## 工具二：AuthUtil — 网关鉴权工具

### 作用

网关的核心鉴权入口，提供两个方法：
- `parseToken(token)`：校验并解析 JWT，得到登录用户信息；
- `checkAuth(antPath, r)`：基于接口 ant 路径做权限判断。

### 方法一：`R<LoginUserDTO> parseToken(String token)`

校验 token 并解析出登录用户。按以下步骤顺序执行，任一步失败即返回对应错误码的 `R`：

| 步骤 | 逻辑 | 失败返回 |
|---|---|---|
| 1. 非空校验 | token 为空直接失败 | `INVALID_TOKEN_CODE / INVALID_TOKEN` |
| 2. 构建 JWT | 用 `JwtSignerHolder.getJwtSigner()` 设置验签器，构建异常则失败 | `INVALID_TOKEN_CODE / INVALID_TOKEN` |
| 3. 签名校验 | `jwt.verify()` 验签失败 | `INVALID_TOKEN_CODE / INVALID_TOKEN` |
| 4. 有效期校验 | `JWTValidator.validateDate()` 校验签发/过期时间 | `EXPIRED_TOKEN_CODE / EXPIRED_TOKEN` |
| 5. 数据格式校验 | payload 中 `user` 字段为空 | `INVALID_TOKEN_CODE / INVALID_TOKEN_PAYLOAD` |
| 6. 解析 DTO | `user` 反序列化为 `LoginUserDTO`（userId / roleId / rememberMe），失败 | `INVALID_TOKEN_CODE / INVALID_TOKEN_PAYLOAD` |
| 7. 成功 | 返回 `R.ok(userDTO)` | — |

### 方法二：`void checkAuth(String antPath, R<LoginUserDTO> r)`

结合 `parseToken` 的返回结果做接口级权限判断：

1. **路径匹配**：用 `AntPathMatcher` 判断请求路径（如 `/admin/**`）是否命中本地缓存的权限路径集合 `paths`；
   - 未命中 → 该接口无权限限制，直接放行；
   - 命中 → 继续后续步骤。
2. **登录校验**：`R.success()` 为 false（token 校验失败）→ 抛 `UnauthorizedException`（未登录）。
3. **获取所需角色**：从权限缓存 `privileges` 中取出该 ant 路径对应的 `PrivilegeRoleDTO.roles`（允许访问的角色集合）。
4. **角色校验**：用户 `roleId` 不在所需角色集合中 → 抛 `ForbiddenException`（无访问权限）。

### 权限缓存与定时刷新

| 字段 | 说明 |
|---|---|
| `privileges` | 内存缓存：ant 匹配路径 → `PrivilegeRoleDTO`（该接口所需角色） |
| `paths` | 所有需要鉴权的 ant 路径集合 |
| `privilegeVersion` | 内存中的权限版本号，用于判断缓存是否需要刷新 |
| `hashOps` | Redis `auth:privileges` Hash 的操作对象，存放全部接口权限数据 |

**`refreshTask()`（`@Scheduled(fixedDelay = 20000)`，每 20 秒执行）**：

1. 读取 Redis 中的权限版本号（key：`version`，与 `auth:privileges` 同库）；
2. 若与内存 `privilegeVersion` 一致 → 数据未变更，跳过；
3. 不一致 → 从 Redis 读取全部权限数据，反序列化为 `PrivilegeRoleDTO` 列表；
4. 重建 `privileges` 映射与 `paths` 集合，并更新 `privilegeVersion`。

这样认证服务后台修改接口权限后，网关最多 20 秒即可生效，无需重启。

### 作用价值

- 集中承载网关层的“登录态校验 + 权限校验”，业务服务无需重复实现鉴权。
- 权限数据来自 Redis（由认证服务维护），支持运行时动态刷新，改权限即生效。

---

## 使用方式

引入依赖后，`AuthAutoConfiguration` 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 被 Spring Boot 自动装配，无需额外配置。

```xml
<dependency>
    <groupId>com.yjw</groupId>
    <artifactId>sc-auth-gateway-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

在网关过滤器中组合使用：

```java
// 1. 解析 token
R<LoginUserDTO> r = authUtil.parseToken(token);
// 2. 校验接口权限（无权限抛异常）
authUtil.checkAuth(requestPath, r);
// 3. 放行并把用户信息透传到下游（如写入 header）
```

### 依赖前置条件

- 注册中心（Nacos 等）中需存在服务名为 `auth-service` 的实例，且提供 `/jwks` 接口返回 Base64 RSA 公钥；
- Redis 中存在 `auth:privileges` Hash（权限数据）与 `version` 版本号（由认证服务写入）。
