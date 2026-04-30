# 前端本地联调说明

前端现在统一通过 `utils/api.js` 连接本地后端。

- 默认后端地址：`http://localhost:8080`
- 默认 API 地址：`http://localhost:8080/api`
- 统一配置文件：`utils/api.js`

## 启动方式

先启动后端：

```powershell
cd C:\Users\c2483\Documents\HBuilderProjects\campus-circle-backend
mvn spring-boot:run
```

然后在 HBuilderX 里运行前端即可。

## 真机调试

如果你用手机真机调试，`localhost` 会指向手机本机，不是你的电脑。这时把后端地址切成你电脑的局域网 IP：

```js
import { setDevBackendOrigin } from '@/utils/api'
setDevBackendOrigin('http://192.168.1.23:8080')
```

之后所有接口、图片和上传都会一起切过去。

恢复默认值：

```js
import { clearDevBackendOrigin } from '@/utils/api'
clearDevBackendOrigin()
```

## 这次已处理的内容

- 统一了接口基地址
- 统一了图片绝对地址
- 统一了上传地址
- 修复了 `request.delete` 缺失的问题
