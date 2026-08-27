# MODEL-P3 企业和配置管理接口验收记录

| 项目 | 结论 |
|------|------|
| 任务编号 | MODEL-P3 |
| 当前状态 | ✅ 已完成 |
| 开始时间 | 2026-08-26 16:11 CST |
| 完成时间 | 2026-08-26 16:46 CST |
| 前置门禁 | MODEL-P2-CUTOVER ✅ |
| 下一阶段 | MODEL-P4 建单策略链路改造 |

## 完成范围

| 范围 | 状态 | 结论 |
|------|------|------|
| 权限迁移 | ✅ | V19 新增企业配置、企业管理、规则组和模板删除权限，赋予内置管理员 |
| 企业管理 | ✅ | 列表、详情、新建、编辑、启停及当前用户可见企业 options |
| 邮箱策略 | ✅ | 保存企业、模板、SLA、规则组、fallback；新增企业筛选和授权邮箱 options |
| 通知模板 | ✅ | 增加企业和模板类型；企业内编码唯一；增加引用删除保护 |
| SLA/日历 | ✅ | 增加企业归属和筛选；默认配置按企业隔离；增加同企业与引用保护 |
| 分配规则组 | ✅ | 新增列表、options、CRUD、启停和引用保护；规则增加 `groupId` |
| 处理人约束 | ✅ | 规则处理人和邮箱默认处理人必须是启用工单处理人，并具备目标邮箱操作权限 |
| 阶段边界 | ✅ | 未提前改造 P4 建单匹配链路，未开发 P5 页面 |

## 验收证据

1. `cd backend && mvn -q test`：后端全量单元测试通过。
2. `NACOS_CONFIG_ENABLED=false mvn -q -Dtest=P3ConfigurationLocalIT test`：本地真实数据库完成企业 → 日历 → SLA → 规则组 → 模板 → 邮箱事务链路，策略字段、可见 options 和引用删除保护通过；业务测试数据随事务回滚。
3. `NACOS_CONFIG_ENABLED=false mvn -q -Dtest=PermissionCutoverLocalIT test`：数据库升级到 V19 后，P2 权限切换矩阵继续通过。
4. 本地配置、Nacos 关闭、统一端口 8080 下，企业列表/选项、邮箱选项、规则组、通知模板、SLA、工作日历 7 个接口均返回 HTTP 200。
5. 本地 Flyway 已由 V18 升级到 V19；本地运行数据库为 MySQL 8.0。MySQL 5.7 兼容结论来自迁移和查询语法静态核对，V19 使用 MySQL 5.7 可用的 `VALUES(...)`，未虚报为 MySQL 5.7 实机验证。
6. 验收结束后已停止临时 8080 后台进程，未改变用户原先的后台关闭状态。

## 关键验收结论

1. 企业 A 的自动回复模板、SLA、工作日历和规则组不能绑定到企业 B 的邮箱或配置。
2. 被邮箱引用的模板、SLA、规则组不能删除；被 SLA/节假日引用的工作日历不能删除。
3. 普通用户企业/邮箱 options 严格来自用户授权，空授权返回空集合；管理员配置接口由动作权限控制。
4. 角色仍只承载功能权限，用户保持单角色、单部门，业务数据范围不回退旧角色范围。
5. 旧表和兼容字段均保留，P4 可以在此基础上改造建单策略链路。

## 主要实现位置

- `backend/src/main/resources/db/migration/V19__enterprise_config_permissions.sql`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/enterprise/EnterpriseService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/mailbox/MailboxService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/assignment/AssignmentRuleGroupService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/assignment/AssignmentRuleService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/template/NotificationTemplateService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/sla/SlaPolicyService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/calendar/WorkCalendarService.java`
- `backend/src/test/java/com/ntn/fziot/mailtrace/integration/P3ConfigurationLocalIT.java`

## 回滚边界

应用可整体回滚到 P2 版本；V19 仅新增权限种子，不删除业务表或历史数据。若回滚应用，V18/V19 表结构和数据继续保留，不做破坏性降级。
