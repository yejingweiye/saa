---
name: api-trace-analyze
description: 分析SpringBoot接口完整请求链路与数据流转；输入Controller文件#方法名或者接口url，输出完整调用链、对象流转、入参、中间处理、返回值、关键分支、涉及的类。多模块项目使用仓库根相对路径。调用：/api-trace-analyze [文件#method] 或者 /api-trace-analyze /api/todo/list
disable-model-invocation: true
---

# 技能：接口全链路追踪分析
## 目标
针对SpringBoot接口，梳理**完整请求从进入Controller到返回响应的全流程与数据流转**。
输出结构严格按照下面固定格式输出，不要随意删减模块。

## 调用输入
两种输入方式：
1. 文件+方法：`/api-trace-analyze chatflow-app/src/main/java/com/yjw/controller/TodoController.java#queryTodoList`
2. API路径：`/api-trace-analyze /api/todo/list`

> 多模块项目路径必须使用仓库根目录的相对路径。

## 输出强制结构
### 1.接口基础信息
- 请求地址、请求方法（GET/POST/PUT）
- Controller类、方法名
- 请求入参：每个参数含义、注解（@RequestBody/@RequestParam/@PathVariable）、DTO实体字段
- 返回对象：返回类型、包装体

### 2.完整调用链路（按执行顺序）
> 按执行顺序列出每一步调用的类和方法，从controller开始，依次往下：
Controller → Service接口 → Service实现 → Mapper/DB操作 / AI‑Graph调用 / 子图调用 → 组装返回。
> 如果存在AOP、过滤器、拦截器、异常处理器，一并标出。

### 3.数据流转（重点）
> 一步一步写清楚对象如何传递、转换、修改：
- 请求进来原始入参对象
- 哪一步做了参数校验/转换
- 哪些对象被赋值、修改；哪些是新创建对象
- 数据库读取/写入了哪些字段
- 如果存在StateGraph调用：state输入、节点流转、子图invoke、state输出、主图state更新
- DTO/VO/Entity之间互相转换关系

### 4.关键分支与条件逻辑
列出代码里的if‑else、条件分支、条件边、异常分支，每个分支会走向什么逻辑，输出什么结果。

### 5.异常与错误路径
- 会抛出什么异常
- 是否有try‑catch处理
- 异常会走到哪个全局异常处理器，返回什么响应

### 6.涉及全部核心文件列表
列出链路中所有被调用的java文件（多模块写完整相对路径）。

### 7.关键风险点/注意点
识别潜在问题：空值、集合、事务、数据库操作、子图调用隔离、参数未校验等。

## 约束规则
1. 不要只贴代码，**重点讲数据流，对象怎么变**。
2. 如果是Spring AI Graph业务，要把StateGraph调用、子图invoke、state输入输出、节点流转完整描述出来。
3. 识别Mapper、数据库表、字段映射。
4. 找不到的类如实说明，不要编造逻辑。
5. 不要省略中间层，即使是简单转发也要写出来。
6. 如果存在lombok、mapstruct转换，标注出来。

## 禁止
- 不要大段复制源码；只贴关键片段。
- 不要跳过子图调用逻辑，子图内部节点链路也要简要展开。
