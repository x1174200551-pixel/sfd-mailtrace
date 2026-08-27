# MODEL-P5 前端联调接口契约

| 项目 | 结论 |
|------|------|
| 契约编号 | MODEL-P5 |
| 状态 | ✅ 已实现并完成本地联调 |
| 日期 | 2026-08-26 |
| 基础路径 | `/api/v1` |
| 数据边界 | 企业归属 + 用户企业/邮箱授权 |
| 用户模型 | 单角色、单主部门 |

## 企业和用户授权

| 方法 | 路径 | 用途 |
|------|------|------|
| `GET` | `/enterprises` | 企业配置列表，支持关键字和启停筛选 |
| `GET` | `/enterprises/options` | 当前用户可见企业选项 |
| `GET` | `/enterprises/{id}` | 企业详情 |
| `POST` | `/enterprises` | 新建企业 |
| `PUT` | `/enterprises/{id}` | 编辑企业 |
| `PATCH` | `/enterprises/{id}/enabled` | 启停企业 |
| `GET` | `/users/{id}/data-grants` | 查询用户企业/邮箱授权 |
| `PUT` | `/users/{id}/data-grants` | 保存用户企业/邮箱授权 |

用户授权保存规则：

1. 管理员始终全部可见，保存时清理逐条授权。
2. 普通用户至少保留一个企业或邮箱授权。
3. 企业授权自动包含该企业现有和未来新增邮箱；同企业的冗余邮箱授权保存时归一化移除。
4. 用户仍只提交一个 `roleCode` 和一个 `departmentId`，不恢复多角色、多部门字段。

## 邮箱和策略配置

1. 邮箱保存增加 `enterpriseId`、`autoReplyTemplateId`、`slaPolicyId`、`assignmentRuleGroupId`、`assignmentFallbackType` 和 `defaultAssigneeId`。
2. 模板、SLA、工作日历和规则组均按企业过滤；跨企业引用由后端拒绝。
3. 规则未命中仅有两种行为：`NONE` 保持待分配，`DEFAULT_ASSIGNEE` 显式使用默认处理人。
4. 新增 `/assignment-rule-groups` 列表、选项、新建、编辑、启停和删除接口；规则列表通过 `groupId` 归属规则组。
5. 历史 `DEFAULT` 规则只保留兼容读取，不允许前端新建；后续建单的未命中行为由邮箱 fallback 决定。

## 查询筛选和返回字段

以下接口支持 `enterpriseId` 和/或 `mailboxId`：

- 工单列表 `/tickets`
- 客户列表 `/customers`
- 工作台 `/dashboard/summary`、`/dashboard/my-todos`、`/dashboard/report`
- 收件记录 `/mail-fetch/logs`
- 发件记录 `/mail-send/logs`

模板、SLA 和工作日历列表支持 `enterpriseId`。客户详情使用 `enterpriseId + email` 定位，避免同邮箱跨企业串档。

工单列表/详情返回：

- `enterpriseId/enterpriseName`
- `mailboxId/mailboxName`
- `autoReplyTemplateId`
- `slaPolicyId`
- `assignmentRuleGroupId`
- `assignmentRuleId`

收发记录返回企业字段；发件记录同时返回 `templateId/templateType`。前端必须按返回快照解释历史工单，不根据当前邮箱配置反推。

## 权限和兼容性

1. 角色页只读写菜单、按钮和接口权限，不展示或提交 `dataScopes`。
2. 所有查询仍由后端数据权限兜底；前端筛选不是权限边界。
3. 普通用户空授权返回空集合，不回退旧角色范围。
4. MySQL 迁移 SQL 保持 5.7 兼容，不使用窗口函数、CTE、JSON 聚合或 MySQL 8 专属 DDL。
5. 本地后端固定使用 8080，前端代理指向 `http://127.0.0.1:8080`。

