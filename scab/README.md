
## 3.scab
spring cloud base 基础复用框架(微服务基础架构),面向教育电商类业务,提供统一网关、认证授权、跨服务调用契约、通用组件与搜索能力。

### 3.1 模块清单与功能

| 模块 | 类型 | 核心功能 |
|---|---|---|
| **sc-common** | 基础公共模块 | 统一响应体 `R<T>`、异常体系(CommonException / UnauthorizedException 等)、通用工具类、MyBatis 自动填充与 MyBatisPlus 分页、Redisson 分布式锁、RabbitMQ 封装、Swagger/knife4j、xxl-job 等自动配置,被所有模块依赖 |
| **sc-api** | 服务调用契约层 | 基于 OpenFeign 定义跨服务客户端:`AuthClient`(→auth-service 查角色)、`UserClient`(→user-service 查用户/登录换取用户详情),含配套 DTO、Sentinel 降级 fallback、Caffeine 缓存 |
| **sc-auth** | 认证授权聚合模块 | 4 个子模块(见下) |
| ├─ **sc-auth-common** | 认证公共常量 | JWT 常量(`authorization`/`user-info`/`token-info` 请求头、算法、TTL)、权限 Redis key、`PrivilegeRoleDTO`(接口 antPath → 所需角色集合) |
| ├─ **sc-auth-gateway-sdk** | 网关端 SDK | `JwtSignerHolder` 从注册中心拉取 auth-service 并请求 `/jwks` 获取 RSA 公钥;`AuthUtil` 验签解析 token + 基于 Redis 权限表做接口权限校验 |
| ├─ **sc-auth-resource-sdk** | 资源端 SDK | 供下游业务服务接入:`UserInfoInterceptor` 读 `user-info` 头写入 `UserContext`;`LoginAuthInterceptor` 校验登录态(未登录 401);`FeignRelayUserInterceptor` 在 Feign 调用时透传 userId |
| ├─ **sc-auth-service** | 认证服务 | 登录/刷新/登出签发 JWT、角色-权限-菜单管理,启动及变更时把接口权限表写入 Redis |
| **sc-gateway** | 网关服务 | 统一入口(端口 10010):路由转发(lb:// + StripPrefix)、链路标识(`requestId`/`x-request-from`)、JWT 鉴权 + 接口权限校验、WebFlux 全局异常、CORS |
| **sc-search** | 搜索服务 | ES 课程搜索(关键词/分类/筛选/排序/高亮)与个性化推荐,通过 MQ 监听课程上/下架、过期事件同步索引 |

### 3.2 依赖关系
sc-common 是地基,箭头表示"依赖":

```
sc-common
 ├─ sc-auth-common ──┬─ sc-auth-gateway-sdk → sc-gateway        (网关端)
 │                   └─ sc-auth-resource-sdk → 各业务服务        (资源端)
 ├─ sc-api ──┬─ sc-auth-service (登录/权限,同时依赖 sc-auth-resource-sdk)
 │           └─ sc-search       (搜索,同时依赖 sc-auth-resource-sdk)
 └─ 其他业务服务
```

### 3.3 数据流转

#### ① 认证链路:登录签发 token
```
前端 → 网关 /as/accounts/login → auth-service
  AccountController → AccountServiceImpl.login
    ├─ Feign: UserClient.queryUserDetail() → user-service 查用户详情(userId/roleId)
    └─ JwtTool.createToken():
         access-token: payload.user = {userId, roleId, rememberMe}
         refresh-token: JTI 存 Redis,写入 HttpOnly Cookie
  → token 返回前端,前端后续请求携带 Authorization 头
```

#### ② 请求鉴权链路:网关校验并透传用户信息
```
前端请求(带 Authorization) → 网关
  1. RequestIdRelayFilter(最先执行):
       生成 requestId → 注入头 requestId、x-request-from: gateway(/ps/notify 回调除外)
  2. AccountAuthFilter(order=1000):
       a. 白名单(默认 /error/**、/jwks、/accounts/login、/accounts/admin/login、/accounts/refresh)直接放行
       b. AuthUtil.parseToken 用 RSA 公钥验签 → LoginUserDTO(userId/roleId)
       c. token 有效 → 注入头 user-info=userId、token-info=原始 token
       d. AuthUtil.checkAuth: antPath = "METHOD:path" 匹配权限表,校验 roleId(无权限抛 403)
  3. 转发 → lb://下游服务(StripPrefix=1 去路由前缀)
```

#### ③ 权限数据流:auth-service → Redis → 网关内存
```
auth-service:
  LoadPrivilegeRunner(启动) / 角色权限变更
    → PrivilegeServiceImpl.listPrivilegeRoles() 从 DB(privilege / role / role_privilege)
      组装 PrivilegeRoleDTO(antPath = method:uri, roles 集合)
    → PrivilegeCache 写入 Redis hash "auth:privileges",version +1
网关 AuthUtil:
  @Scheduled 每 20s 检查 Redis version,变化则把权限表重载到本地内存(privileges / paths)
```

#### ④ 下游服务身份获取链路(资源端 SDK)
```
网关转发请求(带 user-info 头) → 下游业务服务
  UserInfoInterceptor(order 0): 读 user-info → UserContext.setUser(userId)
  LoginAuthInterceptor(order 1): UserContext.getUser() 为空 → 401,否则放行
  服务间 Feign 调用: FeignRelayUserInterceptor 把 UserContext 中的 userId 透传到 user-info 头
```

#### ⑤ 服务间调用链路(Feign / sc-api)
- auth-service 登录时经 `UserClient` 调 user-service 换取用户详情
- 各业务服务(如 sc-search)经 `UserClient` 批量查用户信息、经 `AuthClient` 查角色
- 配合 `user-info` 头与 `FeignRelayUserInterceptor`,用户身份在 Feign 调用链中持续透传

#### ⑥ 搜索数据链路(MQ + ES)
```
course-service 等业务服务发 MQ(课程上/下架、过期、订单事件)
  → sc-search CourseEventListener / OrderEventListener 监听
  → 同步 / 删除 ES 索引(CourseRepository)
前端查询 → 网关 /ss/** → sc-search 查 ES,返回 CourseVO
```

> 一句话总结:前端所有请求经 **sc-gateway** 统一入口;**sc-auth-service** 负责发 token 并把接口权限表刷入 Redis;**sc-auth-gateway-sdk** 在网关验签、校验权限并注入 `user-info` / `token-info` 头;**sc-auth-resource-sdk** 在下游把登录态还原到 `UserContext`;**sc-api** 提供服务间 Feign 调用;**sc-search** 负责 ES 搜索与索引同步。





