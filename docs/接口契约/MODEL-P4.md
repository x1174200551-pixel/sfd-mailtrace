# MODEL-P4 建单策略链路接口契约

| 项目 | 结论 |
|------|------|
| 契约编号 | MODEL-P4 |
| 状态 | ✅ 后端契约已实现 |
| 日期 | 2026-08-26 |
| 基础路径 | `/api/v1` |
| 策略入口 | 来源邮箱 |
| 数据边界 | 企业归属 + 邮箱可操作权限 |

## 建单链路规则

1. 收信建单先校验来源邮箱和所属企业均可用于新业务，再写入 `enterpriseId/mailboxId/customerId/customerEmail`。
2. 客户邮箱统一去除首尾空格并转为小写；客户按 `enterpriseId + email` 原子新增或更新，同一邮箱在不同企业生成不同客户档案。
3. 自动回复、SLA、分配规则只读取来源邮箱显式绑定的配置，不再使用全局默认模板、默认 SLA 或历史 `DEFAULT` 分配规则。
4. 邮箱绑定的模板、SLA、工作日历、规则组必须启用且与邮箱同企业；无绑定或无效配置不阻断建单。
5. 规则处理人和默认处理人必须是启用工单处理人，并具备来源邮箱操作权限。

## 空配置和兜底行为

| 邮箱配置 | 建单行为 |
|----------|----------|
| 未开启自动回复或未绑定可用模板 | 不发送；工单记录 `AUTO_REPLY_SKIPPED` 事件 |
| 未绑定可用 SLA | `slaPolicyId/slaResponseDeadline/slaResolveDeadline` 为空 |
| 未绑定可用规则组或无规则命中 | 默认保持 `PENDING_ASSIGN` |
| `assignmentFallbackType=NONE` | 即使配置了默认处理人也保持待分配 |
| `assignmentFallbackType=DEFAULT_ASSIGNEE` | 默认处理人有效且有目标邮箱操作权限时转为 `PROCESSING` |

## 工单返回字段

现有工单列表、工作台工单摘要和工单详情接口增加以下字段：

- `enterpriseId/enterpriseName`
- `slaPolicyId`
- `autoReplyTemplateId`
- `assignmentRuleGroupId`
- `assignmentRuleId`

其中策略 ID 是建单时实际绑定或使用的快照。`assignmentRuleGroupId` 记录来源邮箱当时绑定的规则组，`assignmentRuleId` 只在规则实际命中时有值。

## 自动邮件与发件日志

内部发送接口扩展工单和模板元数据，自动回复、分配通知、SLA 提醒写入发件日志时同步保存：

- `enterpriseId`
- `mailboxId`
- `ticketId`
- `sendType`
- `templateId`
- `templateType`

自动回复在尝试 SMTP 发送前写入 `ticket.autoReplyTemplateId`，即使 SMTP 失败也保留本次策略选择证据；SMTP 失败仍不回滚已创建工单。

## 兼容性边界

1. 本阶段没有新增对外 Controller 路径，现有工单接口保持兼容，只扩展响应字段。
2. 客户原子写入使用 MySQL 5.7 支持的 `INSERT ... ON DUPLICATE KEY UPDATE` 和 `VALUES(...)`，不使用 MySQL 8 专属语法。
3. 历史全局默认字段和 `DEFAULT` 规则数据继续保留，但不再参与新建工单策略选择。
4. 页面如何展示企业、邮箱和策略快照属于 MODEL-P5；P5 必须先完成新模型原型确认。
