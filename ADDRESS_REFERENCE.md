# 统一地址配置说明

这份文档专门说明：我们已经统一过的地址配置，现在分别写在哪些文件里。

## 1. 统一接口基地址

统一定义文件：

- `frontend/utils/api.js`

关键内容：

- `DEFAULT_BACKEND_ORIGIN = 'http://localhost:8080'`
- `getApiBaseUrl()`

实际请求入口：

- `frontend/utils/request.js`
  - `getBaseUrl()`

已经接入这套配置的前端接口文件：

- `frontend/api/user.js`
- `frontend/api/common.js`

也就是说，前端以后如果还要新增接口，优先继续走：

- `frontend/utils/api.js`
- `frontend/utils/request.js`

不要再在页面里手写一份新的后端地址。

## 2. 统一图片绝对地址

统一定义文件：

- `frontend/utils/api.js`

关键方法：

- `toAbsoluteUrl()`

目前已经在这些位置使用：

- `frontend/components/PostCard.vue`
- `frontend/pages/post/detail.vue`
- `frontend/pages/errand/detail.vue`

这意味着：

- 后端返回相对图片路径时，前端会统一补成完整地址
- 以后如果还有新的页面显示后端图片，也建议继续复用 `toAbsoluteUrl()`

## 3. 统一上传地址

前端上传入口：

- `frontend/utils/request.js`
  - `upload()`
- `frontend/api/common.js`
  - `uploadImage()`
  - `uploadBatchImages()`

后端对应接口：

- `backend/src/main/java/com/campus/campus_backend/controller/CommonController.java`
  - `POST /api/common/upload`
  - `POST /api/common/upload/batch`

也就是说，头像上传、图片上传这类需求，原则上都应该优先走这套入口，不要再新写一套上传地址。

## 4. 后端本地服务地址

后端服务端口和本地地址配置在：

- `backend/src/main/resources/application.properties`
  - `server.port=8080`
- `backend/src/main/resources/application-local.properties`
  - `server.url=http://localhost:8080`
- `backend/src/main/resources/application-local-mysql.properties`
  - `server.url=http://localhost:8080`

默认情况下：

- 后端本地运行端口是 `8080`
- 前端默认就会请求 `http://localhost:8080/api`

## 5. 真机调试时怎么切换地址

如果是手机真机调试，需要把前端请求地址改成你电脑的局域网 IP。

使用位置：

- `frontend/utils/api.js`

相关方法：

- `setDevBackendOrigin('http://<你的局域网IP>:8080')`
- `clearDevBackendOrigin()`

示例：

```js
import { setDevBackendOrigin } from '@/utils/api'
setDevBackendOrigin('http://192.168.1.23:8080')
```

恢复默认：

```js
import { clearDevBackendOrigin } from '@/utils/api'
clearDevBackendOrigin()
```

## 6. 后续开发时的约定

后面如果继续开发，请尽量遵守下面这几个约定：

1. 新接口不要在页面里手写后端地址
2. 新图片展示不要自己拼地址，优先复用 `toAbsoluteUrl()`
3. 新上传功能优先接入现有上传入口
4. 如果真的要新增地址配置，优先集中写到 `frontend/utils/api.js`
