# 校园圈子 2.0 项目协作说明

这个仓库是我们项目的整合开发仓库，包含前后端完整代码，方便组员各自在本地独立开发。

目录结构：

- `backend/`：Spring Boot 后端
- `frontend/`：uni-app 前端
- `ADDRESS_REFERENCE.md`：统一地址配置说明
- `DEVELOPMENT_GUIDE.md`：多人协作开发建议

## 1. 组员第一次接手时，推荐这样做

如果你是第一次把项目拉到本地，建议按下面顺序操作：

1. 先启动后端
2. 后端先使用默认本地数据库模式
3. 后端启动成功后，再用 HBuilderX 运行前端

这样最省事，因为不依赖共享服务器，也不依赖共享远程数据库。

## 2. 本地开发前需要准备什么

后端需要：

- JDK 21
- Maven 3.9+
- 建议本机有 Redis，默认端口 `6379`

前端需要：

- HBuilderX

如果你想让后端连接本地 MySQL，还需要：

- MySQL 8+

## 3. 后端数据库怎么配置

后端现在支持两种本地开发方式。

### 方式 A：默认本地 H2 数据库

这是目前最推荐的方式，也是默认方式。

相关配置文件：

- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-local.properties`

当前关键配置：

- 默认激活环境：`local`
- 本地数据库文件：`backend/data/campus-circle.mv.db`
- 本地上传目录：`backend/uploads`
- 本地日志目录：`backend/logs`
- H2 控制台：`http://localhost:8080/h2-console`

启动命令：

```powershell
cd backend
mvn spring-boot:run
```

第一次启动会自动完成：

- 自动建表
- 初始化默认分类
- 初始化管理员账号

默认管理员账号：

- 用户名：`admin`
- 密码：`admin123`

### 方式 B：连接本地 MySQL

如果你更习惯用 MySQL，可以切换到本地 MySQL 模式。

相关配置文件：

- `backend/src/main/resources/application-local-mysql.properties`

建议步骤：

1. 确保本机 MySQL 已安装并启动
2. 准备一个本地可登录账号
3. 启动时切换到 `local-mysql`

示例命令：

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local-mysql"
$env:LOCAL_DB_URL="jdbc:mysql://localhost:3306/campus_circle?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&createDatabaseIfNotExist=true"
$env:LOCAL_DB_USERNAME="root"
$env:LOCAL_DB_PASSWORD="你的MySQL密码"
mvn spring-boot:run
```

仓库里也有一个辅助 SQL：

- `backend/scripts/init-local-mysql.sql`

补充说明：

- 本地 MySQL 模式下已经开启 `ddl-auto=update`
- 也就是说空库第一次启动时，会自动按实体建表
- 每个组员都可以用自己的本地 MySQL，不需要共享数据库

## 4. Redis 要不要配

Redis 主要用于：

- 在线状态
- token 黑名单

默认配置位置：

- `backend/src/main/resources/application-local.properties`
- `backend/src/main/resources/application-local-mysql.properties`

默认地址：

- Host：`localhost`
- Port：`6379`

如果本机没有 Redis，后端核心开发通常也还能继续，只是部分在线状态和黑名单能力会降级。

## 5. 怎么确认后端启动成功

后端启动后，建议至少检查下面几个地址：

1. Swagger 文档：`http://localhost:8080/swagger-ui.html`
2. 健康检查：`http://localhost:8080/actuator/health`
3. 如果你用 H2：`http://localhost:8080/h2-console`

## 6. 前端怎么启动

前端目录是：

- `frontend/`

请用 HBuilderX 打开 `frontend/` 后运行。

前端现在默认请求本地后端：

```text
http://localhost:8080/api
```

如果你是在电脑本机调试，一般不需要额外改地址。

## 7. 如果是真机调试，怎么改前端地址

如果你用手机真机调试，`localhost` 指向的是手机本机，不是你的电脑。

这时要把前端的后端地址切到你电脑的局域网 IP，比如：

```js
import { setDevBackendOrigin } from '@/utils/api'
setDevBackendOrigin('http://192.168.1.23:8080')
```

恢复默认地址：

```js
import { clearDevBackendOrigin } from '@/utils/api'
clearDevBackendOrigin()
```

## 8. 现在统一过的地址都在哪里

### 统一接口基地址

定义位置：

- `frontend/utils/api.js`
  - `DEFAULT_BACKEND_ORIGIN`
  - `getApiBaseUrl()`

实际请求入口：

- `frontend/utils/request.js`
- `frontend/api/user.js`

### 统一图片绝对地址

定义位置：

- `frontend/utils/api.js`
  - `toAbsoluteUrl()`

当前使用位置：

- `frontend/components/PostCard.vue`
- `frontend/pages/post/detail.vue`
- `frontend/pages/errand/detail.vue`

### 统一上传地址

前端使用位置：

- `frontend/utils/request.js`
  - `upload()`
- `frontend/api/common.js`
  - `uploadImage()`
  - `uploadBatchImages()`

后端对应接口：

- `backend/src/main/java/com/campus/campus_backend/controller/CommonController.java`

更详细的说明见：

- `ADDRESS_REFERENCE.md`

## 9. 我们现在推荐的协作方式

因为我们没有共享数据库，也没有共享线上数据，所以推荐这样协作：

1. 每个人都用自己的本地数据库
2. 接口字段变化时，前后端尽量在同一个分支一起改
3. 不要依赖“我本地这条数据的 id 正好是多少”
4. 如果一个功能依赖初始化数据，要把初始化方式写进代码或文档
5. 提交 PR 时，最好写清楚接口变化和测试方式

更详细的协作建议见：

- `DEVELOPMENT_GUIDE.md`

## 10. 仓库结构

```text
campus_circle2.0/
  backend/
  frontend/
  README.md
  ADDRESS_REFERENCE.md
  DEVELOPMENT_GUIDE.md
```
