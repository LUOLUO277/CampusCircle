# Canvas IAM 接入交接说明

更新时间：2026-05-06

## 目标

把 `campus_circle2.0` 里的 Canvas 接入从“用户手填 token 调 API”改成：

- 用户在前端填写 Canvas 地址、统一认证用户名/学号、密码
- 后端模拟登录同济 IAM / Canvas
- 后端抓取页面内容
- 同步结果写入当前用户自己的通知流

## 当前结论

目前这条链路已经走通到同济 IAM 的真实登录页，并且账号密码表单已经成功提交到了 IAM。

已经确认的跳转链如下：

1. `https://canvas.tongji.edu.cn/`
2. `/login`
3. `/login/openid_connect`
4. `https://iam.tongji.edu.cn/idp/oauth2/authorize?...`
5. `https://iam.tongji.edu.cn/idp/AuthnEngine?...`
6. `https://iam.tongji.edu.cn/idp/authcenter/ActionAuthChain?...`
7. 提交表单后跳到 `https://iam.tongji.edu.cn/idp/profile/OAUTH2/AuthorizationCode/SSO?...`

当前失败点不在跳转链，也不在基础 cookie 保存，而是在 IAM 登录提交之后，IAM 没有识别出完整的登录认证上下文。

最新解析出的 IAM 错误正文是：

`提示：无登录认证用户信息，请联系管理员。`

这说明问题已经缩小到：

- 登录表单真实提交字段还原不完整
- 提交顺序、多值字段、按钮值或前端脚本生成字段还没有完全模拟到位

## 已完成改造

### 后端

- 用户级 Canvas 绑定模型：
  `backend/src/main/java/com/campus/campus_backend/domain/UserCanvasBinding.java`

- 用户绑定接口：
  `backend/src/main/java/com/campus/campus_backend/controller/CanvasBindingController.java`

  已提供：
  - `GET /api/canvas-binding`
  - `PUT /api/canvas-binding`
  - `POST /api/canvas-binding/sync`
  - `DELETE /api/canvas-binding`

- 绑定业务：
  `backend/src/main/java/com/campus/campus_backend/service/info/CanvasBindingService.java`

  已支持：
  - 保存账号密码
  - 手动同步
  - 同步状态写入 `lastSyncStatus` / `lastSyncMessage`
  - 同步结果写入当前用户自己的通知流

- Canvas 抓取器：
  `backend/src/main/java/com/campus/campus_backend/service/info/CanvasSessionFetcher.java`

  已完成的关键工作：
  - 手动跟踪 302，不依赖 Java 自动跳转
  - 记录每一步 `status / location / contentType / bodyLength / snippet`
  - 解析 IAM 登录页中的 `form` 和 `input`
  - 支持 HTML entity 解码
  - cookie 存储从简单 `Map<String, String>` 改成按 `name + domain + path` 管理的简易 cookie jar
  - 已增加请求 cookie、响应 cookie、最终 cookie jar 的诊断输出
  - 已增加 IAM 错误页标题和正文解析
  - 已增加登录字段注入日志：
    - `credential-source-fields`
    - `credential-inputs`
    - `credential-action`
    - `credentialTargets`
    - `credentialDefaults`
    - `final-page-title`
    - `final-page-text`

- 通知流已支持“公共通知 + 当前用户私有通知”：
  - `backend/src/main/java/com/campus/campus_backend/domain/AggregatedNotice.java`
  - `backend/src/main/java/com/campus/campus_backend/service/info/InfoCenterService.java`

- 后端最近一次编译状态：
  - `mvn -q -DskipTests compile` 通过

### 前端

- Canvas 绑定页已改成账号密码模式：
  `frontend/pages/profile/canvas-binding.vue`

  当前字段：
  - Canvas 地址
  - 学号或统一认证用户名
  - 密码
  - 可选课程 ID

- API：
  `frontend/api/info-center.js`

- 页面路由：
  `frontend/pages.json`

- 个人中心入口：
  `frontend/pages/profile/index.vue`

## 最新日志结论

2026-05-06 最新一轮日志的关键信息如下：

- IAM 登录页可见字段包括：
  - `j_username`
  - `j_password`
  - `j_checkcode`
  - `fs12_username`
  - `fs1_username`
  - `fs1_password`
  - 多个认证相关 hidden 字段

- 当前后端已成功把表单 POST 到：
  `AuthnEngine?currentAuth=urn_oasis_names_tc_SAML_2.0_ac_classes_BAMUsernamePassword...`

- 提交后服务端返回：
  `302 -> /idp/profile/OAUTH2/AuthorizationCode/SSO?...`

- 最终错误页无额外表单，正文明确为：
  `提示：无登录认证用户信息，请联系管理员。`

## 当前最可能的问题

按现有信息，最可能是下面几类之一：

1. IAM 登录页不是纯 HTML 表单，前端 JS 会在点击登录前改写或补充字段。
2. 表单里存在同名字段、多值字段、字段顺序依赖，但当前实现仍然是 `Map<String, String>`，会丢掉重复项和原始顺序。
3. 某些字段必须保留原值，某些字段必须删除，当前还没有完全对齐浏览器真实请求。
4. 登录按钮本身可能带有额外提交语义，例如 `op`、按钮 value、onclick 逻辑。
5. `j_checkcode`、`authenticatedFlag`、`authenticatedUsername`、`currentAuthen`、`authenServletPath` 等字段仍未处理准确。

## 建议下一步

### 方案 A：优先做，抓浏览器真实请求

这是最有效的路径。

建议直接用浏览器开发者工具或抓包工具，在人工登录一次同济 IAM 时记录：

1. `ActionAuthChain` 页面最终渲染后的 DOM
2. 点击登录按钮时真实发出的 POST URL
3. POST body 的完整字段、重复字段、字段顺序、字段值
4. 是否存在前置 AJAX
5. 是否有 JS 在提交前改写这些字段：
   - `op`
   - `j_authMethodID`
   - `currentAuthen`
   - `authenServletPath`
   - `authenticatedFlag`
   - `authenticatedUsername`

拿到这份真实请求之后，对照后端日志里的：

- `credential-source-fields`
- `credentialTargets`
- `credentialDefaults`

逐项补齐。

### 方案 B：继续改当前 Java 模拟

如果暂时抓不到浏览器真实请求，建议下一步直接改：

1. 把表单提交结构从 `Map<String, String>` 改成保序、支持重复键的结构。
   目标是保留：
   - 字段原始顺序
   - 同名字段多次出现
   - 按钮字段原始值

2. `parseInputs` 之后不要立刻去重。

3. 重点排查这些字段的真实用途：
   - `op`
   - `j_authMethodID`
   - `authMethodIDs`
   - `currentAuthen`
   - `authenServletPath`
   - `authenticatedFlag`
   - `authenticatedUsername`
   - `checkcodeInputID`
   - `passwordInputID`

4. 如果页面里有按钮点击逻辑或内联脚本改 payload，需要把那部分逻辑手动移植到 Java。

### 方案 C：必要时换技术路线

如果同济 IAM 强依赖前端脚本和复杂交互，可以考虑：

1. 后端改成无头浏览器登录后抓取页面，例如 Playwright / Selenium。
2. 把“获取登录态”的逻辑拆成独立服务。

这个方案成本更高，但如果最终确认表单提交流程 heavily depends on JS，它会比纯 HTTP 模拟更稳。

## 继续排查时重点看哪些日志

当前 `CanvasSessionFetcher` 已经会输出：

- `credential-source-fields=...`
- `credential-inputs=...`
- `credential-action=...`
- `credentialTargets=...`
- `credentialDefaults=...`
- `credential-submit ...`
- `final-page-title=...`
- `final-page-text=...`

继续排查时，建议至少保留以上几段作为最小日志集。

## 交接建议

接手这个任务时，不要回退到旧的 token 方案，也不要先去改通知流。

当前基础设施已经搭好：

- 后端绑定模型已完成
- 前端入口和表单已完成
- 用户私有通知流已完成
- Canvas 登录主链路已走到 IAM 最后一段

剩余核心工作只有一件事：

**把同济 IAM 的真实登录提交请求完整还原。**
