# P1-W0-C01-login 登录接口契约验收记录

| 项目 | 结论 |
|------|------|
| 任务编号 | P1-W0-C01-login |
| 当前状态 | ✅ 已完成 |
| 验收时间 | 2026-07-22 16:27 CST |
| 验收人 | AI |
| 前置原型 | PG-01 ✅，谭总已确认 |

## 交付物

- `docs/接口契约/P1-W0-C01-login.md`
- 覆盖 `POST /api/v1/auth/login`、`GET /api/v1/auth/me`、`POST /api/v1/auth/logout`
- 明确默认账号、响应字段、错误码和前端 token 存储规则

## 验证

- 已对照 PG-01 登录页控件完成字段映射。
- 已按 `application.yml` 启动后端并完成接口冒烟：
  - 健康检查：`GET /api/v1/system/health` 返回 `200 / code=0`
  - 错误密码：`POST /api/v1/auth/login` 返回 `401 / code=40101`
  - 正确登录：`admin/admin123` 返回 `200 / code=0 / tokenType=Bearer`
  - 当前用户：携带 token 调用 `/api/v1/auth/me` 返回管理员账号信息
  - 未登录当前用户：不带 token 调用 `/api/v1/auth/me` 返回 `401 / code=40102`
