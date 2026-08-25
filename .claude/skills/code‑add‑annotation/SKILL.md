---
name: code-add-annotation
description: 给Java代码补充必要注解、JavaDoc注释；支持整个项目、单个文件、指定类、指定方法。只补必要注解，不生成无意义废话注释。调用：/code-add-annotation [文件路径] ，不传路径默认处理当前打开文件。
disable-model-invocation: true
---

# 技能：代码补全必要注解与JavaDoc
## 目标
为Java SpringBoot项目补充**必要的注解与文档注释**，遵循原则：
1. 只补**真正必要**的注解和注释，拒绝无意义废话注释，不重复描述代码显而易见逻辑。
2. 优先补：类注释、public接口/方法注释、参数说明、返回值说明、异常说明；私有方法内部逻辑如果易懂不强制加注释。
3. Spring相关类补充对应组件注解（@Service、@Component、@Repository、@RestController等）；接口、DTO、VO、Entity、Mapper、Service、Controller区分对待。
4. DTO实体类：补充类JavaDoc，重要字段补充@ApiModelProperty / @Schema注释；getter/setter不要重复写注释。
5. 工具类：增加类注释，说明工具用途；静态方法补充JavaDoc。
6. 不修改原有业务逻辑，**只增加注解、JavaDoc，不改动代码业务实现**。
7. 如果已有注释，判断是否够用；够用就保留，不要重复生成。

## 输入调用格式
1. `/code-add-annotation`：处理当前打开的文件
2. `/code-add-annotation src/main/java/com/xxx/XXX.java`：处理指定单个文件
3. `/code-add-annotation src/main/java/com/xxx/service/`：处理该目录下全部java文件
4. 可以指定方法：`/code-add-annotation src/main/java/com/xxx/XXX.java#methodName`，只处理这个方法

## 不同类型代码处理规则
### 1. Controller / RestController
- 类上增加JavaDoc，说明该控制器职责。
- 每个接口方法：`@GetMapping/@PostMapping`保持；补充JavaDoc：接口功能、入参说明、返回说明。
- @ApiOperation / @Operation 按需补充。

### 2. Service接口 & 实现类
- Service接口：接口上加JavaDoc，每个抽象方法写JavaDoc（功能、参数、返回）。
- 实现类：实现方法**不要重复复制接口JavaDoc**，使用`@inheritDoc`即可，除非实现有特殊业务逻辑才追加注释。

### 3. DTO / VO / Entity
- 类注释说明这个实体用途。
- 关键字段增加注释，简单字段不用每一行都写。
- lombok实体不要给get/set写注释。

### 4. Mapper / Repository
- 接口增加简短类注释。

### 5. 普通工具类
- 类注释说明用途，注意是否不可实例化；私有构造可以补充注释。
- public static方法补充JavaDoc。

### 6. 普通方法
- public：必须JavaDoc，@param、@return，抛出受检异常补充@throws。
- protected：按需补充。
- private：逻辑简单不写注释；复杂逻辑写简短行内注释，不强制JavaDoc。

## 输出格式
1. 输出完整修改后的代码，使用java代码块。
2. 简要说明本次增加了哪些注解/注释。
3. 如果文件不需要修改，直接说明：`该文件注解已经完备，无需修改`。

## 禁止行为
1. 不要给for循环、if判断写废话注释。
2. 不要修改原有业务代码、变量名、逻辑。
3. 不要重复生成一模一样的注释。
4. 不要大段冗余描述，注释尽量简短精炼。
