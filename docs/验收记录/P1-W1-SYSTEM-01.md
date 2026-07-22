# P1-W1-SYSTEM-01 编号规则实施验收记录

| 项目 | 结论 |
|------|------|
| 任务编号 | P1-W1-BE-09 / P1-W1-FE-05 |
| 当前状态 | 🧪 前端页面待谭总验收 |
| 验收时间 | 2026-07-22 19:57 CST |
| 原型 Gate | PG-04 ✅ |
| 接口契约 | `docs/接口契约/P1-W0-C01-system.md` |

## 实施范围

- 后端接口：`GET /api/v1/sys-params/ticket-number-rule`
- 后端接口：`POST /api/v1/sys-params/ticket-number-rule/preview`
- 后端接口：`PUT /api/v1/sys-params/ticket-number-rule`
- 前端页面：系统管理 / 编号规则。
- 范围裁剪：不开发通用系统参数表，不暴露参数键、新增参数、行内参数保存。

## 关键修复

- `requestApi` 底层自动从本地登录态读取 token，并为非登录接口兜底添加 `Authorization` 请求头。
- 编号规则查询、预览、保存接口均沿用统一请求头逻辑，避免再次出现“已登录但业务请求提示未登录”的问题。

## 验证结果

```json
{
  "mvnTest": "passed",
  "frontendBuild": "passed",
  "frontendLint": "passed",
  "apiSmoke": {
    "getNext": "TCK-20260722-0001",
    "previewNext": "ABC-20260722-000001",
    "saveNext": "TCK-20260722-0001"
  }
}
```

## 待谭总确认

请在页面复验编号规则查询、生成预览、保存确认和保存后不退出登录。确认后将 `P1-W1-FE-05` 标记为 ✅。
