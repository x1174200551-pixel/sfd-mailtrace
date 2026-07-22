# P1-W0-C01-template 通知模板接口契约

| 项目 | 结论 |
|------|------|
| 契约编号 | P1-W0-C01-template |
| 当前状态 | ✅ 已完成 |
| 日期 | 2026-07-22 |
| 前置 | PG-03 ✅ |
| 覆盖范围 | 模板列表、模板新增、模板保存、变量预览、启停状态 |

## 通用规则

- 所有接口需要 `Authorization: Bearer <token>`。
- 第一版仅 `ADMIN` 可编辑通知模板；非管理员返回 `403 / code=40302`。
- 模板编码唯一；系统内置模板由 Flyway 初始化，自定义模板由管理员在前端新增。
- 支持变量：`{ticket_no}`、`{subject}`、`{customer_email}`、`{customer_name}`、`{assignee_name}`、`{mailbox_email}`、`{sla_deadline}`、`{ticket_link}`。

## GET /api/v1/notification-templates

### 查询参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 按模板编码、名称模糊搜索 |
| enabled | boolean | 否 | `true` 启用，`false` 停用 |

### 响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [
      {
        "id": 1,
        "templateCode": "AUTO_REPLY",
        "templateName": "自动回执模板",
        "subjectTpl": "您的工单已创建：{ticket_no}",
        "contentTpl": "您好，您的邮件已进入工单系统。",
        "enabled": true,
        "updatedAt": "2026-07-22T17:20:00"
      }
    ],
    "summary": {
      "totalTemplates": 5,
      "enabledTemplates": 4,
      "disabledTemplates": 1,
      "availableVariables": 8
    },
    "variables": [
      { "key": "{ticket_no}", "label": "工单号", "sampleValue": "TCK-20260722-0001" }
    ]
  }
}
```

## POST /api/v1/notification-templates

### 请求

```json
{
  "templateCode": "CUSTOM_NOTICE",
  "templateName": "自定义通知模板",
  "subjectTpl": "通知：{ticket_no}",
  "contentTpl": "您好，工单 {ticket_no} 有新的通知。",
  "enabled": true
}
```

| 字段 | 必填 | 校验 |
|------|------|------|
| templateCode | 是 | 1-64 字符；仅支持大写字母、数字和下划线，且必须以字母开头；不可重复 |
| templateName | 是 | 1-64 字符 |
| subjectTpl | 是 | 1-512 字符 |
| contentTpl | 是 | 1-10000 字符 |
| enabled | 是 | true/false |

## PUT /api/v1/notification-templates/{id}

### 请求

```json
{
  "templateName": "自动回执模板",
  "subjectTpl": "您的工单已创建：{ticket_no}",
  "contentTpl": "您好，您的邮件已进入工单系统。",
  "enabled": true
}
```

| 字段 | 必填 | 校验 |
|------|------|------|
| templateName | 是 | 1-64 字符 |
| subjectTpl | 是 | 1-512 字符 |
| contentTpl | 是 | 1-10000 字符 |
| enabled | 是 | true/false |

## POST /api/v1/notification-templates/preview

### 请求

```json
{
  "subjectTpl": "您的工单已创建：{ticket_no}",
  "contentTpl": "工单主题：{subject}"
}
```

| 字段 | 必填 | 校验 |
|------|------|------|
| subjectTpl | 是 | 1-512 字符 |
| contentTpl | 是 | 1-10000 字符 |
| sampleData | 否 | 调试扩展字段；正式前端不展示输入，默认由后端示例数据渲染 |

### 响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "subject": "您的工单已创建：TCK-20260722-0001",
    "content": "工单主题：订单物流查询"
  }
}
```

## 错误码

| code | 场景 | HTTP |
|------|------|------|
| 40001 | 参数不合法、变量格式不支持 | 400 |
| 40302 | 非管理员或越权操作 | 403 |
| 40401 | 模板不存在 | 404 |

## 实现与验证

- 后端实现：`NotificationTemplateController`、`NotificationTemplateService`、`NotificationTemplateEntity`、`V3__notification_templates.sql`。
- Service 方法规范：公共方法保留用途注释，方法体内使用 `// 1、...`、`// 2、...` 标明逻辑执行步骤。
- 前端实现：AppShell 内容区的通知模板页面，覆盖模板列表、新建、编辑保存、启停、变量插入、预览和权限态。
- 验证命令：`mvn -q clean test`、`npm run build`、`npm run lint`。
- 接口冒烟：模板列表、变量 8 个、新增模板、预览变量替换、保存模板、非法变量 400、处理人访问 403。
