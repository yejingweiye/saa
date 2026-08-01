## scba.jks 生成

### 1.执行命令
keytool -genkeypair \
-alias scab \
-keyalg RSA \
-keysize 2048 \
-storetype JKS \
-keystore scba.jks \
-validity 3650

JKS 生成参数与yml配置映射表

| keytool 参数 | 参数含义 | 项目yml对应配置 |
| ---- | ---- | ------------ |
| `-alias scab` | 密钥别名 | `encrypt.key-store.alias: scab` |
| `-keystore scba.jks` | 密钥库文件名称 | `encrypt.key-store.location: classpath:scba.jks` |
| 密钥库口令 | jks 整体访问密码 | `encrypt.key-store.password: sc123321` |
| 密钥口令 | jks内部RSA私钥密码 | `encrypt.key-store.secret: sc123321` |
| `-keyalg RSA` | 密钥加密算法 | JWT RS256 签名配套算法 |
| `-keysize 2048` | 密钥位数 | 通用安全标准长度 |
| `-validity 3650` | 证书有效期（单位：天） | 3650 = 10年，过期需重新生成密钥 |

### 2.交互输入密码（和配置完全对应）
密钥库口令：sc123321（对应配置 password）
确认库口令：sc123321
姓名、单位、城市等信息随便填写 / 直接回车跳过:填的都是sc
密钥口令（私钥密码）：sc123321（对应配置 secret）
确认密钥口令：sc123321
执行完成后，当前目录生成 scba.jks，放到项目 resources 目录。

### 3.安全问题解决
```yaml
services:
  auth:
    image: sc-auth:1.0.0
    volumes:
      # 挂载密钥文件
      - ./config/scba.jks:/app/resources/scba.jks
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - JKS_STORE_PWD=sc123321
      - JKS_KEY_PWD=sc123321
    ports:
      - "8080:8080"
```
