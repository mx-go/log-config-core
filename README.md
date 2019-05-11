# log-config-core
**项目必须使用logback作为日志输出组件。**具体使用可参照：

# 引入Maven坐标

```xml
<dependency>
	<groupId>com.github.max</groupId>
	<artifactId>log-config-core</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

# 使用

jar包中的`LogbackServlet`通过`web-fragment`会自动注册到服务中，jar包提供以下功能：

- 获取服务所有logger level信息
- 获取单个logger level信息
- 设置指定logger的logger level
- 获取所有日志文件信息
- 查询某日志文件最近N行日志

## 获取服务所有logger level信息

通过访问：http://ip:prot/logback/all		   **GET**		   无参数

示例：http://localhost:8080/logback/all

## 获取单个logger level信息

通过访问：http://ip:prot/logback/getLogger		**GET**		

- 参数**logger**[必填]：logger名称

示例：http://localhost:8080/logback/getLogger?logger=com.rainbowhorse.open

## 设置指定logger的logger level

通过访问：http://ip:prot/logback/setLevel		GET		

参数：

- logger[必填]：logger名称
- level[选填]：旧的日志级别
- newLevel[必填]：新的日志级别

其中level、newLevel可选值有"OFF"、 "TRACE"、"DEBUG"、"INFO"、"WARN"、"ERROR"

示例：http://localhost:8080/logback/setLevel?newLevel=DEBUG&logger=com.rainbowhorse.open

## 获取所有日志文件信息

通过访问：http://ip:prot/logback/getLogFiles		**GET**		无参数

示例：http://localhost:8080/logback/getLogFiles

## 查询某日志文件最近N行日志

通过访问：http://ip:prot/logback/peekFile		**GET**		

参数：

- **file**[必填]：需要tail的文件名称。

- **num**[选填]：tail的行数，,默认1000。

示例：http://localhost:8081/logback/peekFile?file=admin.log&num=10



