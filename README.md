# i18n-spring-boot-starter
Spring Boot 国际化组件

0.引入依赖
代码在本地打包后 
给需要国际化的工程引入
```xml

<dependency>
    <groupId>com.bdkjzx.project</groupId>
    <artifactId>i18n-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
```properties

#添加国际化
spring.ex.i18n.enable=false
spring.ex.i18n.mark=false
spring.ex.i18n.default-locale=zh-CN
spring.ex.i18n.data-source=e6yun_main_mysql
spring.ex.i18n.config-table=config_i18n_message_e6yun3
```
1.配置项
```properties

#添加国际化
spring.ex.i18n.enable=false
spring.ex.i18n.mark=false
spring.ex.i18n.default-locale=zh-CN
spring.ex.i18n.data-source=e6yun_main_mysql
spring.ex.i18n.config-table=config_i18n_message_e6yun3
```

2.初始化国际化配置表
```sql

CREATE TABLE `config_i18n_message` (
  `code` varchar(128)   NOT NULL,
  `zh-CN` varchar(128)  DEFAULT NULL,
  `zh-TW` varchar(128)  DEFAULT NULL,
  `en-US` varchar(1024)   DEFAULT NULL COMMENT '英文',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='国际化配置表'


```

使用
```java
I.n("操作成功")
```
或者在返回的统一结果对象上,以下是个示例，你需要加在你的项目的统一响应中
```java
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private ErrorDetails error;

    public ApiResponse() {
    }
    /**
     * message给消息进行国际化包装
     * @param message
     */
    public ApiResponse(int code, String message, T data, ErrorDetails error) {
        this.code = code;
        this.message = I.n(message);
        this.data = data;
        this.error = error;
    }

    // Getter and Setter methods

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 给消息进行国际化包装
     * @param message
     */
    public void setMessage(String message) {
        this.message = I.n(message);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }
}

```
