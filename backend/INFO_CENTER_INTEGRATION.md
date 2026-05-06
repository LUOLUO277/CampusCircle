# 信息中心真实接入说明

当前版本已经支持把真实来源配置进信息中心，不再局限于 `MOCK_*`。

## 1. 当前支持的抓取策略

- `WECHAT_RSS`
  适合已经有 RSS 镜像的公众号来源，例如 RSSHub、自建镜像站或校内历史文章镜像。
- `WECHAT_HTML`
  适合可公开访问的公众号文章镜像列表页。
- `ACADEMIC_HTML`
  适合教务公告列表页、学院通知页、部门公告页。
- `ACADEMIC_RSS`
  适合教务系统本身提供 RSS 的情况。
- `CANVAS_API`
  适合学校开放 Canvas token 或你能拿到用户级 token 的情况。
- `CANVAS_HTML`
  适合只能轮询 Canvas 页面、不能直接走 API 的情况。

## 2. 关键现实约束

### 公众号

“全网搜索某个公众号文章”这件事不要按搜索引擎思路做。微信官方没有稳定开放的公众号全文检索接口，直接抓 `mp.weixin.qq.com` 也很容易失效。

第一阶段建议这样落地：

1. 后台先录入“已知公众号来源”，不要做全网搜索。
2. 优先接 RSSHub 或学校自己的公众号历史文章镜像。
3. 如果没有 RSS，就录入一个镜像列表页，走 `WECHAT_HTML`。
4. 真失效时继续用人工补录兜底。

### 教务

教务公告通常比公众号更适合做稳定轮询。优先找公开公告页，直接轮询 HTML 列表；如果需要登录，再把 Cookie 放进 `headers`。

### Canvas

如果学校开放 API，优先用 `CANVAS_API`。如果没有，就退回 `CANVAS_HTML`，轮询日历页、待办页或公告页。

## 3. 后台录入示例

### 公众号 RSS

`type = WECHAT`

`fetchStrategy = WECHAT_RSS`

`fetchConfigJson`:

```json
{
  "rssUrl": "https://rsshub.app/wechat/mp/<biz>",
  "maxItems": 20,
  "category": "活动",
  "tags": ["公众号", "校园"]
}
```

### 教务 HTML

`type = ACADEMIC`

`fetchStrategy = ACADEMIC_HTML`

`fetchConfigJson`:

```json
{
  "listUrl": "https://example.edu.cn/notice/list",
  "baseUrl": "https://example.edu.cn",
  "maxItems": 20,
  "category": "教务",
  "keyword": "通知 公告",
  "tags": ["教务"]
}
```

如果页面需要登录：

```json
{
  "listUrl": "https://example.edu.cn/notice/list",
  "baseUrl": "https://example.edu.cn",
  "maxItems": 20,
  "headers": {
    "Cookie": "JSESSIONID=xxx",
    "User-Agent": "CampusCircle/1.0"
  }
}
```

### Canvas API

`type = CANVAS`

`fetchStrategy = CANVAS_API`

`fetchConfigJson`:

```json
{
  "baseUrl": "https://canvas.example.edu.cn",
  "token": "canvas-access-token",
  "announcementCourseIds": [101, 202],
  "includeGlobalAnnouncements": true,
  "includeTodo": true,
  "maxItems": 30,
  "category": "课程"
}
```

## 4. 现在的抓取行为

- 管理端可新增来源、编辑基本信息、手动触发抓取。
- 后端定时任务每 30 分钟轮询一次 `ACTIVE` 来源。
- 抓到的内容会统一进入 `aggregated_notice`，由现有提取逻辑自动补摘要、截止时间、标签、原文链接。

## 5. 建议你的下一步

1. 先别做“搜索任意公众号”。
2. 先挑 3 个真实源打通：
   - 1 个 RSSHub 公众号
   - 1 个公开教务公告页
   - 1 个 Canvas API 或 Canvas 页面
3. 跑通后再补“来源申请 + 管理员审核录入”闭环。

如果你要，我下一步可以继续帮你把“同济教务公告”或“同济 Canvas”按你学校实际页面结构定制到可直接用的配置或解析逻辑。
