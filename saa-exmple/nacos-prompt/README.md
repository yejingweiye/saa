## Nacos 配置添加
1. 启动 Nacos 服务；
2. 写入配置，dataId 为：spring.ai.alibaba.configurable.prompt
3. 在配置中写入如下配置：

    ```json
    [
      {
        "name": "author",
        "template": "列出 {author} 有名的著作",
        "model": {
          "key": "李白"
        }
      }
    ]
    ```

## Nacos 动态更新 Nacos 的 Prompt 配置，再次查看请求查看效果

变更 Prompt 为：

   ```json
   [
     {
       "name":"author",
       "template":"介绍 {author}，列出其生平经历和文学成就",
       "model":{
         "key":"余华"
       }
     }
   ]
   ```

## spring.config.import: "optional:nacos:prompt-config.json"
Spring Boot 3 把 nacos dataId = prompt-config.json 的内容拉进来，作为一个外部属性源合并进 Spring Environment
src/main/resources/prompt-config.json 本地这份只是「参考内容/示例」   
