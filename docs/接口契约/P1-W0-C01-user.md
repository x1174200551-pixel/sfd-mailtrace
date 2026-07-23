# P1-W0-C01-user 用户管理接口契约

| 项目 | 结论 |
|------|------|
| 契约编号 | P1-W0-C01-user |
| 当前状态 | ✅ 已完成 |
| 日期 | 2026-07-22 |
| 前置 | PG-02 ✅ |
| 覆盖范围 | 用户列表、新建、编辑、启停、重置密码 |

## 通用规则

- 所有接口需要 `Authorization: Bearer <token>`。
- 第一版仅 `ADMIN` 可访问用户管理接口；非管理员返回 `403 / code=40302`。
- 导出功能暂不纳入第一版。
- 账号创建后不允许修改，避免影响登录和审计追踪。

## GET /api/v1/users

### 查询参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | number | 否 | 页码，从 1 开始，默认 1 |
| size | number | 否 | 每页条数，默认 10，最大 100 |
| keyword | string | 否 | 按账号、姓名、邮箱模糊搜索 |
| roleCode | string | 否 | `ADMIN` / `AGENT` |
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
        "account": "admin",
        "displayName": "系统管理员",
        "email": "admin@ntn.fziot",
        "roleCode": "ADMIN",
        "enabled": true,
        "lastLoginAt": "2026-07-22T16:10:00",
        "createdAt": "2026-07-22T15:00:00",
        "updatedAt": "2026-07-22T16:10:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10,
    "pages": 1,
    "summary": {
      "totalUsers": 1,
      "enabledUsers": 1,
      "disabledUsers": 0,
      "adminUsers": 1,
      "agentUsers": 0
    }
  }
}
```

## POST /api/v1/users

### 请求

```json
{
  "account": "agent01",
  "displayName": "客服一号",
  "email": "agent01@ntn.fziot",
  "roleCode": "AGENT",
  "password": "agent123",
  "enabled": true
}
```

| 字段 | 必填 | 校验 |
|------|------|------|
| account | 是 | 2-64 字符，仅允许字母、数字、点、下划线、中划线；唯一 |
| displayName | 是 | 1-64 字符 |
| email | 是 | 邮箱格式，最大 128 字符 |
| roleCode | 是 | `ADMIN` / `AGENT` |
| password | 是 | 6-128 字符 |
| enabled | 否 | 默认 true |

## PUT /api/v1/users/{id}

### 请求

```json
{
  "displayName": "客服一号",
  "email": "agent01@ntn.fziot",
  "roleCode": "AGENT",
  "enabled": true
}
```

账号不可编辑。管理员不能把自己的角色改为 `AGENT`，也不能停用自己。

## PATCH /api/v1/users/{id}/enabled

```json
{
  "enabled": false
}
```

管理员不能停用自己。停用后用户不可登录。

## POST /api/v1/users/{id}/reset-password

```json
{
  "password": "newpass123"
}
```

密码重置后接口只返回成功，不回传明文密码。

## 错误码

| code | 场景 | HTTP |
|------|------|------|
| 40001 | 参数不合法 | 400 |
| 40302 | 非管理员或越权操作 | 403 |
| 40401 | 用户不存在 | 404 |
| 40901 | 账号已存在 | 409 |

## 实现与验证

- 后端实现：`UserController`、`UserService`、`UserEntity`、`OperationLogEntity`。
- 前端实现：AppShell 内容区的用户管理页面，覆盖筛选、分页、新建、编辑、启停、重置密码、空态、加载态、错误态、权限态。
- 验证命令：`mvn -q test`、`npm run build`、`npm run lint`。
- 接口冒烟：管理员登录、用户分页、新建用户、重复账号 409、编辑、停用后不可登录、启用、重置密码后可登录、处理人访问用户管理 403。
