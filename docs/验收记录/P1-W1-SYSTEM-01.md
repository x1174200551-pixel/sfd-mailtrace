# P1-W1-SYSTEM-01 编号规则实施验收记录

| 项目 | 结论 |
|------|------|
| 任务编号 | P1-W1-BE-09 / P1-W1-FE-05 |
| 当前状态 | ✅ 已完成 |
| 验收时间 | 2026-07-23 09:45 CST |
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
  "groupSwitch": "passed",
  "boundaryPanelRemoved": "passed",
  "apiSmoke": {
    "getNext": "TCK-20260722-0001",
    "previewNext": "ABC-20260722-000001",
    "saveNext": "TCK-20260722-0001"
  }
}
```

## 谭总确认

2026-07-23 09:45 CST，谭总确认编号规则页面通过，`P1-W1-FE-05` 标记为 ✅。

## 反馈修复

| 时间 | 反馈 | 处理 |
|------|------|------|
| 2026-07-22 20:03 CST | 左侧配置分组无法点击切换 | 增加分组选中态和内容切换；非编号规则分组切换后展示只读维护边界，不进入通用参数编辑 |
| 2026-07-23 09:35 CST | 底部系统参数边界区块可以拿掉 | 已移除底部系统参数边界区块；非编号规则分组内的只读说明保留 |
