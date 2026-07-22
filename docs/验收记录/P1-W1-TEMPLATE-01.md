# P1-W1 通知模板实施验收记录

| 项目 | 结论 |
|------|------|
| 任务编号 | P1-W0-C01-template / P1-W1-BE-08 / P1-W1-FE-04 |
| 当前状态 | 🧪 前端页面待谭总验收 |
| 验收时间 | 2026-07-22 17:33 CST |
| 验收人 | AI，自测通过；等待谭总页面确认 |
| 原型依据 | `docs/原型设计/7、邮件工单系统-通知模板原型.html/png` |

## 交付物

- 后端接口：
  - `GET /api/v1/notification-templates`
  - `PUT /api/v1/notification-templates/{id}`
  - `POST /api/v1/notification-templates/preview`
- 初始化数据：
  - `V3__notification_templates.sql`
  - `AUTO_REPLY`
  - `ASSIGN_NOTIFY`
  - `AGENT_REPLY`
  - `SLA_WARNING`
  - `SLA_BREACH`
- 前端页面：
  - 左侧菜单“系统管理 / 通知模板”
  - 模板统计卡片
  - 模板列表与搜索
  - 模板编辑区
  - 启停开关
  - 变量点击插入正文
  - 预览面板和示例数据
  - 保存模板二次确认
- 契约：`docs/接口契约/P1-W0-C01-template.md`

## 退回调整记录

| 时间 | 反馈 | 调整 |
|------|------|------|
| 2026-07-22 17:40 CST | 点击预览提示登录状态失效 | 前端识别 401/40102 后清理旧登录态并回到登录页提示重新登录，避免停留在无效会话页面 |
| 2026-07-22 17:40 CST | 变量名需要注释 | 变量面板改为展示变量名、中文含义和示例值 |
| 2026-07-22 17:40 CST | 底部状态说明可以不要 | 移除页面底部“空态/加载态/错误态/权限态”说明卡片 |
| 2026-07-22 17:44 CST | 登录后点击预览仍提示登录失效并退出 | 修复前端 `requestApi` 请求头合并顺序，确保 Authorization 与 `Content-Type: application/json` 同时发送 |
| 2026-07-22 17:54 CST | 新增模板按钮位置与保存按钮位置不顺手 | 左侧模板列表标题栏新增“新建”；中间编辑器标题栏和底部操作栏放“保存/创建”；页面总标题右侧仅保留刷新 |

## 验证命令

```bash
mvn -q clean test
npm run build
npm run lint
```

## 反馈复验结果

```json
{
  "login": { "status": 200, "code": 0 },
  "preview": {
    "status": 200,
    "code": 0,
    "subject": "工单 TCK-20260722-0100",
    "content": "主题：前端代理复验，处理人：王敏"
  }
}
```

## 新增与布局复验结果

```json
{
  "login": { "status": 200, "code": 0 },
  "create": { "status": 200, "code": 0, "id": 7 },
  "list": { "status": 200, "code": 0, "count": 1 },
  "preview": {
    "status": 200,
    "code": 0,
    "subject": "临时通知 TCK-20260722-0200",
    "content": "主题：新增模板冒烟，客户：customer@example.com"
  },
  "cleanup": { "id": 7, "isDeleted": 1 }
}
```

## 接口冒烟结果

```json
{
  "adminLogin": { "status": 200, "code": 0 },
  "list": { "status": 200, "code": 0, "count": 5, "variables": 8 },
  "preview": {
    "status": 200,
    "code": 0,
    "subject": "工单 TCK-20260722-0099",
    "content": "主题：模板冒烟，处理人：王敏"
  },
  "update": { "status": 200, "code": 0, "templateCode": "AUTO_REPLY" },
  "invalid": { "status": 400, "code": 40001 },
  "agentList": { "status": 403, "code": 40302 }
}
```

## 服务状态

```json
{
  "backend": 200,
  "frontend": 200
}
```

## 待谭总确认

请在前端页面检查通知模板的布局、模板切换、变量插入、预览、保存确认和权限态。确认后将 `P1-W1-FE-04` 标为 ✅，再进入下一项。
