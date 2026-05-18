# 并行开发手册

适用范围：`课程攻略知识库`、`交易信用/跑腿/二手增强`、`信息订阅中心`

## 1. 先说结论

这三个方向可以并行开发，而且当前仓库结构适合按模块拆人，不需要三个人反复改同一批文件。

前提只有四个：

1. 不要把新功能都塞进现有“首页帖子分类”那条链路里。
2. 每个人只负责自己模块的前后端目录和接口文件。
3. `pages.json`、首页入口、用户表这类共享文件只在约定窗口修改。
4. 所有开发都走 GitHub 分支 + PR，不直接往 `main` 推。

## 2. 项目现状分析

### 2.1 技术栈

- 前端：`uni-app + Vue`
- 后端：`Spring Boot 3 + Spring Data JPA + Spring Security`
- 数据库：本地开发默认 H2，也支持 MySQL

### 2.2 当前模块边界

前端已经按模块拆过一层：

- 帖子：`frontend/pages/post/*`、`frontend/api/post.js`
- 跑腿：`frontend/pages/errand/*`、`frontend/api/errand.js`
- 信息订阅：`frontend/pages/info-center/*`、`frontend/api/info-center.js`
- 首页聚合：`frontend/pages/index/index.vue`

后端也是按业务拆控制器：

- 帖子：`backend/.../controller/PostsController.java`
- 跑腿：`backend/.../controller/ErrandsController.java`
- 用户：`backend/.../controller/UsersController.java`
- 信息订阅：`backend/.../controller/InfoCenterController.java`

结论很明确：

- `信息订阅中心`已经是一条相对独立的链路，最适合单人独立开发。
- `跑腿/信用/二手交易增强`可以围绕 `User`、`Errand`、帖子商品信息单独推进。
- `课程攻略知识库`不要强绑现有 `Category`，应该单独建课程域模型。

## 3. 三人分工建议

推荐按“业务模块”分，而不是按“一个人写前端、一个人写后端、一个人写数据库”分。

### A 号同学：课程攻略知识库

负责范围：

- 课程结构化页面
- 课程页下挂载课程相关帖子
- 经验沉淀扩展为校园经验库

建议主目录：

- 前端：`frontend/pages/course/*`、`frontend/pages/experience/*`、`frontend/api/course.js`
- 后端：`backend/.../controller/CourseController.java`
- 后端：`backend/.../service/course/*`
- 后端：`backend/.../domain/Course*.java`
- 后端：`backend/.../repository/Course*.java`
- 后端：`backend/.../dto/course/*`

### B 号同学：信用体系 + 跑腿 + 二手增强

负责范围：

- 用户信用分
- 跑腿任务效率优化
- 二手交易增强

建议主目录：

- 前端：`frontend/pages/errand/*`、`frontend/pages/profile/*` 中信用相关页面、`frontend/api/errand.js`、新增 `frontend/api/credit.js`
- 后端：`backend/.../controller/ErrandsController.java`
- 后端：`backend/.../controller/UsersController.java`
- 后端：`backend/.../controller/TradeController.java` 或 `CreditController.java`
- 后端：`backend/.../domain/User.java`、`Errand.java`、新增信用/交易实体

### C 号同学：信息订阅中心

负责范围：

- 订阅源管理
- 订阅筛选与推荐
- 展示体验和后台管理

建议主目录：

- 前端：`frontend/pages/info-center/*`、`frontend/pages/admin/info-center.vue`、`frontend/api/info-center.js`
- 后端：`backend/.../controller/InfoCenterController.java`
- 后端：`backend/.../controller/AdminInfoCenterController.java`
- 后端：`backend/.../service/info/*`
- 后端：`backend/.../domain/AggregatedNotice.java`
- 后端：`backend/.../domain/SubscriptionSource.java`
- 后端：`backend/.../domain/NoticeSubscription.java`

## 4. 架构原则

### 4.1 课程模块不要复用 `Category` 充当课程库

当前 `Category` 明显是社区帖子分类，不是课程实体。

如果把课程直接塞进 `categories`，会出现三个问题：

1. 课程数量大，首页分类导航会失控。
2. 课程的教师、学分、性质、资料列表无法自然表达。
3. 后续“经验库”也会被迫继续滥用帖子分类。

课程模块建议独立建表，至少包含：

- `Course`
- `CourseMaterial`
- `CourseReviewStat` 或课程评分聚合表
- `KnowledgeTopic` 或经验主题表

帖子与课程的关系建议用以下两种方式之一：

1. 给 `Post` 增加 `courseId`
2. 建中间表 `CoursePostRelation`

如果后面要支持“一帖多标签”，优先中间表。

### 4.2 信息订阅中心保持独立，不要和课程模块混表

信息订阅中心当前已经有自己的：

- controller
- service
- domain
- 前端页面

这条链路应继续独立演进，不建议为了“统一知识库”去直接合并到课程库数据表中。

### 4.3 信用体系归用户域，不要散落在跑腿和帖子逻辑里

信用分要归到 `User` 或单独信用表管理，跑腿、二手、评价只是信用事件来源。

建议至少拆出：

- 用户当前信用分/等级
- 信用变更流水
- 交易互评记录
- 失信处罚记录

## 5. GitHub 分支策略

主分支建议：

- `main`：稳定可运行
- 不允许直接 push 到 `main`

功能分支建议：

- `feature/course-knowledge-base`
- `feature/trust-errand-trade`
- `feature/info-center-upgrade`

如果某个模块内部过大，再继续拆二级分支：

- `feature/course-entity-api`
- `feature/course-frontend-pages`
- `feature/credit-score-engine`

规则：

1. 每个人长期只维护自己的主功能分支。
2. 每次提交 PR 的粒度要小，优先“一个可验证子功能一个 PR”。
3. 每天开始开发前先 `pull --rebase` 自己分支对应的最新基线。
4. 任何共享文件修改都要提前在群里报备。

## 6. 共享文件冲突清单

下面这些文件最容易冲突，必须控制修改节奏：

- `frontend/pages.json`
- `frontend/pages/index/index.vue`
- `frontend/components/TabBar.vue`
- `frontend/components/PostCard.vue`
- `frontend/pages/publish/index.vue`
- `frontend/pages/publish/Post.vue`
- `frontend/api/index.js`
- `backend/.../config/DataInitializer.java`
- `backend/.../domain/User.java`
- `backend/.../domain/Post.java`
- `backend/.../domain/Category.java`
- `backend/src/main/resources/application*.properties`
- `backend/.../security/SecurityConfig.java`

处理规则：

1. 能新建文件就不要改共享文件。
2. 共享文件只指定一个 owner 改，其他人通过 PR 合并。
3. `pages.json` 统一在每天固定时段集中合并一次。
4. `User.java` 这种实体改动必须先在群里确认字段名和含义。

## 7. 为了减少冲突，必须这样写代码

### 前端规则

1. 新模块优先新建独立 API 文件，例如 `frontend/api/course.js`，不要把所有接口都继续堆进 `frontend/api/index.js`。
2. 新页面统一放新目录，例如 `frontend/pages/course/`，不要把课程页写进现有帖子页。
3. 首页只做“入口”，不要把完整业务都堆进 `frontend/pages/index/index.vue`。
4. 课程帖子发布如果需要选课程，优先扩展发布流程中的独立弹层或独立页面，不要重写整套发帖页。

### 后端规则

1. 新业务优先新建 `Controller + Service + Repository + Domain`，不要把所有逻辑继续塞进 `PostsController`。
2. 课程知识库和经验库单独建域模型。
3. 信用体系单独建服务层，不要把信用计算直接散写在 `ErrandsController` 和 `UsersController` 中。
4. 信息订阅中心继续沿用现有 `service/info/*` 分层。

## 8. 建议的开发顺序

虽然是并行开发，但仍然有一个最稳的时序。

### 第 0 步：先做一次共享约定 PR

由一名负责人先提交一个很小的 PR，只做这些事：

- 确认三个模块分支名
- 建立新目录骨架
- 写接口命名约定
- 写数据库命名约定

不要在这个 PR 里直接塞业务代码。

### 第 1 步：三人并行开发各自主模块

A 号：

- 课程表结构
- 课程详情页
- 课程关联帖子接口

B 号：

- 信用分字段与流水
- 跑腿状态增强
- 二手商品结构化字段

C 号：

- 信息订阅中心优化
- 管理端增强
- 订阅体验优化

### 第 2 步：集中处理三个共享入口

集中合并以下改动：

- `frontend/pages.json`
- 首页入口
- 个人中心入口
- 首页搜索/推荐入口

这一步不要并行乱改。

### 第 3 步：联调合并

按以下顺序合并更稳：

1. 先合 `信息订阅中心`
2. 再合 `信用/跑腿/二手增强`
3. 最后合 `课程攻略知识库`

原因：

- 信息订阅中心最独立，风险最低。
- 信用体系会改用户和交易逻辑，影响中等。
- 课程知识库新增页面、发帖关联、入口改动最多，适合最后收口。

## 9. PR 规范

每个 PR 描述至少写清楚：

- 这个 PR 做了什么
- 改了哪些接口
- 改了哪些表或实体字段
- 前端新增了哪些页面路由
- 如何本地验证
- 是否影响其他模块

PR 标题建议：

- `[Course] add course entity and detail api`
- `[Credit] add trust score fields and transaction logs`
- `[InfoCenter] optimize subscription management`

## 10. 每天协作节奏

建议每天固定两次同步：

1. 中午同步一次接口和字段变更
2. 晚上同步一次合并状态和次日计划

同步格式统一为：

- 今天改了哪些文件
- 新增了哪些接口/字段
- 哪些文件别人暂时不要碰
- 当前阻塞是什么

## 11. 本项目下最推荐的“低冲突”落地方案

### 课程知识库

新增独立课程体系，不替换现有帖子体系底层，只在展示层和关联层整合帖子。

建议：

- 课程有自己的详情页
- 帖子可选填 `courseId`
- 课程页聚合相关帖子
- 经验库与课程库共用“知识内容聚合”思路，但表不要一开始硬合并

### 信用/跑腿/二手

以 `User + Errand + Post(productInfo)` 为基础演进，不动信息订阅链路。

建议：

- 信用分先做可计算、可展示、可限制
- 跑腿先补状态、时间窗口、预测占位
- 二手先补分类、标签、价格区间、交易完成确认

### 信息订阅中心

继续单独推进，不依赖前两个模块完成。

建议：

- 保持当前独立 API 文件
- 保持当前独立页面目录
- 只在首页保留入口和少量聚合卡片

## 12. 最后确认

按这份手册执行，你们三个人可以并行开发，而且冲突可控。

真正需要严格控制的不是“能不能并行”，而是以下三个共享点：

1. 路由注册
2. 首页入口
3. 用户/帖子基础实体

只要这三类改动不同时随意改，最后合并不会乱。
