# MODEL-P3 企业和配置管理接口契约

| 项目 | 结论 |
|------|------|
| 契约编号 | MODEL-P3 |
| 状态 | ✅ 后端契约已实现 |
| 日期 | 2026-08-26 |
| 基础路径 | `/api/v1` |
| 用户模型 | 单角色、单部门 |
| 权限模型 | 角色控制功能权限；企业/邮箱授权控制业务数据可见范围 |

## 通用规则

1. 除登录外均需 `Authorization: Bearer <token>`，响应沿用 `BasicResult`。
2. 配置列表和写接口按各自 `*:read/create/update/enable/delete` 权限校验，不能只依赖前端隐藏按钮。
3. `/enterprises/options` 和 `/mailboxes/options` 面向业务表单，不要求配置管理权限，但只返回当前用户已授权范围。
4. 新建或修改配置只能选择启用企业；新业务选择可通过 `operationalOnly=true` 进一步限制到启用企业、启用邮箱。
5. 模板、SLA、工作日历、分配规则组和邮箱引用必须属于同一企业；不一致返回业务校验错误。
6. 被邮箱引用的模板、SLA、分配规则组禁止删除；被 SLA 或节假日引用的工作日历禁止删除。

## 企业

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/enterprises?keyword=&enabled=` | `enterprise:read` | 企业配置列表及启停统计 |
| GET | `/enterprises/{id}` | `enterprise:read` | 企业详情 |
| GET | `/enterprises/options?enabled=` | 登录用户 | 当前用户可见企业选项 |
| POST | `/enterprises` | `enterprise:create` | 新建企业，企业名称唯一 |
| PUT | `/enterprises/{id}` | `enterprise:update` | 编辑企业 |
| PATCH | `/enterprises/{id}/enabled` | `enterprise:enable` | 启用或停用企业 |

企业保存字段：`enterpriseName/contactName/contactEmail/contactPhone/enabled/remark`。

## 邮箱策略入口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/mailboxes?enterpriseId=&keyword=&status=&enabled=&page=&size=` | `mailbox:read` 或 `menu:mailboxes` | 按企业和授权邮箱分页查询 |
| GET | `/mailboxes/options?enterpriseId=&operationalOnly=` | 登录用户 | 可见或可操作邮箱选项 |
| POST | `/mailboxes` | `mailbox:create` | 新建邮箱并保存策略引用 |
| PUT | `/mailboxes/{id}` | `mailbox:update` | 编辑邮箱及策略引用 |

邮箱在原字段基础上增加：

- `enterpriseId`：必填。
- `autoReplyTemplateId`：可空，只能引用同企业且启用的 `AUTO_REPLY` 模板。
- `slaPolicyId`：可空，只能引用同企业且启用的 SLA。
- `assignmentRuleGroupId`：可空，只能引用同企业且启用的规则组。
- `assignmentFallbackType`：`NONE` 或 `DEFAULT_ASSIGNEE`。
- `defaultAssigneeId`：设置时必须是启用的工单处理人，并具备该邮箱操作权限。

## 通知模板、SLA 和工作日历

| 资源 | 查询变化 | 保存变化 | 删除保护 |
|------|----------|----------|----------|
| `/notification-templates` | 增加 `enterpriseId/templateType` | 增加 `enterpriseId/templateType`；编码在企业内唯一 | 邮箱引用时禁止删除；增加 `notification_template:delete` |
| `/sla-policies` | 增加 `enterpriseId` | 增加 `enterpriseId`；日历必须同企业 | 邮箱引用时禁止删除或变更企业 |
| `/work-calendars` | 增加 `enterpriseId` | 增加 `enterpriseId`；默认日历在企业内唯一 | SLA/节假日引用时禁止删除，引用存在时禁止变更企业 |

`isDefault` 字段暂保留兼容，但 P4 建单只按邮箱绑定的 SLA 选择，不再读取全局默认策略。

## 分配规则组和规则

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/assignment-rule-groups?enterpriseId=&keyword=&enabled=` | `assignment_rule_group:read` | 规则组列表 |
| GET | `/assignment-rule-groups/options?enterpriseId=&enabled=` | `assignment_rule_group:read` | 规则组选项 |
| POST | `/assignment-rule-groups` | `assignment_rule_group:create` | 新建企业规则组 |
| PUT | `/assignment-rule-groups/{id}` | `assignment_rule_group:update` | 编辑规则组 |
| PATCH | `/assignment-rule-groups/{id}/enabled` | `assignment_rule_group:enable` | 启停规则组 |
| DELETE | `/assignment-rule-groups/{id}` | `assignment_rule_group:delete` | 无邮箱和规则引用时删除 |

规则保存和返回增加 `groupId`，列表增加 `groupId` 筛选。P3 配置页面不再允许新建 `DEFAULT` 规则，兜底统一由邮箱 `assignmentFallbackType/defaultAssigneeId` 表达；同一排序请求只能包含同一规则组的规则。

## P3 新权限编码

`menu:enterprise_config`、`menu:enterprises`、`enterprise:read/create/update/enable`、`assignment_rule_group:read/create/update/enable/delete`、`notification_template:delete`。

以上权限由 `V19__enterprise_config_permissions.sql` 初始化并赋予内置管理员角色，SQL 保持 MySQL 5.7 兼容。

## 阶段边界

本契约只覆盖配置管理。收信建单时真正按邮箱应用模板、SLA 和规则组，以及工单策略快照写入，属于 MODEL-P4。
