# 本地开发说明

这个项目现在支持两种本地启动方式：

1. 默认方式：`local` 配置，使用本地 H2 文件数据库，不依赖旧服务器。
2. MySQL 方式：`local-mysql` 配置，连接你自己的本地 MySQL。

## 1. 默认启动

直接在后端目录运行：

```powershell
mvn spring-boot:run
```

默认会使用 `application-local.properties`：

- 数据库：`./data/campus-circle.mv.db`
- 文件上传目录：`./uploads`
- 日志目录：`./logs`
- H2 控制台：`http://localhost:8080/h2-console`

首次启动会自动建表，并初始化：

- 管理员账号：`admin`
- 管理员密码：`admin123`
- 默认分类数据

## 2. 使用本地 MySQL

如果你希望后端连本地 MySQL，先准备一个可登录的本地账号，然后运行：

```powershell
$env:SPRING_PROFILES_ACTIVE="local-mysql"
$env:LOCAL_DB_URL="jdbc:mysql://localhost:3306/campus_circle?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&createDatabaseIfNotExist=true"
$env:LOCAL_DB_USERNAME="你的本地MySQL用户名"
$env:LOCAL_DB_PASSWORD="你的本地MySQL密码"
mvn spring-boot:run
```

说明：

- `ddl-auto=update` 已开启，空库会自动建表。
- Redis 默认连 `localhost:6379`；如果 Redis 没启动，核心接口仍可调试，只是在线状态和 token 黑名单能力会降级。

## 3. 前端联调

前端请求路径已经是相对地址 `/api`，本地后端启动在 `8080` 后即可联调。

如果你用 HBuilderX/uni-app 本地运行前端，需要保证开发代理或同源访问最终能把 `/api/*` 转到本地后端。

## 4. 已验证结果

我已在 2026-04-30 用默认 `local` 配置实际启动过后端，结果如下：

- Spring Boot 正常启动在 `18080`
- H2 本地数据库文件已生成：`data/campus-circle.mv.db`
- JPA 自动建表成功
- `admin / admin123` 初始化成功
