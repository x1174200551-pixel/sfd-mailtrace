# P1-W0-C01-login 登录接口契约

| 项目 | 结论 |
|------|------|
| 契约编号 | P1-W0-C01-login |
| 当前状态 | ✅ 已完成 |
| 日期 | 2026-07-22 |
| 前置 | PG-01 ✅ |
| 覆盖范围 | 登录页、当前用户、退出登录 |

## 默认账号

| 字段 | 值 | 说明 |
|------|----|------|
| account | `admin` | 初始化管理员账号 |
| password | `admin123` | 初始密码，仅用于本地开发和验收；入库必须为 BCrypt 哈希 |
| roleCode | `ADMIN` | 管理员 |

## POST /api/v1/auth/login

### 请求

```json
{
  "account": "admin",
  "password": "admin123",
  "rememberMe": true
}
```

| 字段 | 类型 | 必填 | 前端来源 | 校验 |
|------|------|------|----------|------|
| account | string | 是 | 账号输入框 | 1-64 字符，去除首尾空格 |
| password | string | 是 | 密码输入框 | 1-128 字符 |
| rememberMe | boolean | 否 | 记住我 | 默认 false |

### 响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "jwt-token",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "account": "admin",
      "displayName": "系统管理员",
      "email": "admin@ntn.fziot",
      "roleCode": "ADMIN"
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | 后续接口使用的 Bearer Token |
| tokenType | string | 固定 `Bearer` |
| expiresIn | number | token 有效期秒数 |
| user | object | 当前登录用户摘要 |

### 错误码

| code | 场景 | 前端提示 |
|------|------|----------|
| 40001 | 账号或密码为空 | 请输入账号和密码后再登录 |
| 40101 | 账号或密码错误 | 账号或密码错误，请检查后重试 |
| 40301 | 账号停用 | 该账号已停用，请联系管理员处理 |

## GET /api/v1/auth/me

### 请求头

```http
Authorization: Bearer <token>
```

### 响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "account": "admin",
    "displayName": "系统管理员",
    "email": "admin@ntn.fziot",
    "roleCode": "ADMIN"
  }
}
```

| code | 场景 | 前端处理 |
|------|------|----------|
| 40102 | token 缺失、过期或非法 | 清理本地 token 并回到登录页 |

## POST /api/v1/auth/logout

第一版后端保持无状态，退出登录由前端清理 token；接口返回成功，便于后续接入 token 黑名单或审计日志。

```json
{
  "code": 0,
  "message": "ok",
  "data": null
}
```

## 前端存储规则

| 场景 | 规则 |
|------|------|
| rememberMe=true | token 存 `localStorage` |
| rememberMe=false | token 存 `sessionStorage` |
| 登录成功 | 保存 token 与 user，跳转工作台 |
| 登录失败 | 不保存 token，保持在登录页 |
| 401 | 清理 token 与 user，回登录页 |
