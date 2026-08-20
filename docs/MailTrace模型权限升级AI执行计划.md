# MailTrace 模型权限升级 AI 执行计划

创建时间：2026-08-20  
适用项目：`sfd-mailtrace`  
输入文档：

- `/Users/tanzhixing/Documents/Obsidian Vault/邮件工单系统/mailtrace/企业邮箱业务配置关系模型.md`
- `/Users/tanzhixing/Documents/Obsidian Vault/邮件工单系统/mailtrace/企业邮箱配置关联调整执行计划.md`

计划状态：已按当前仓库复核，待业务确认后执行  
目标：在现有 MailTrace 工单、邮箱、SLA、分配规则、自动回复模板、用户、角色、组织能力基础上，升级为“企业业务归属 + 邮箱策略入口 + 用户企业/邮箱授权”的权限模型，并完成对应前端调整。

## 1. 复核结论

### 1.1 文档方向可采用

两份 Obsidian 文档的核心方向和当前系统演进目标一致：

1. 企业作为业务数据归属边界，不做物理多租户。
2. 邮箱归属企业，并作为自动回复模板、SLA、分配规则组的策略绑定入口。
3. 部门和处理人继续作为全局运营组织，不挂企业。
4. 角色只控制菜单、按钮、接口能力，不再控制业务数据范围。
5. 业务数据权限统一收口为“当前用户可见邮箱”。
6. 工单创建时固化企业、邮箱、模板、SLA、规则组、命中规则等快照字段。

### 1.2 当前代码尚未落地的关键差异

当前仓库仍是旧权限模型，不能直接进入企业字段扩展：

| 领域 | 当前代码现状 | 对新模型的影响 |
|---|---|---|
| 角色数据范围 | `mt_role_data_scope` 已建表，`PermissionService` 仍读取 `dataScopes`，登录和角色接口仍返回/保存 `dataScopes` | 必须先退场，否则会和用户企业/邮箱授权叠加，导致权限结果不可解释 |
| 工单数据权限 | `DataScopeService` 当前按角色 `ALL/SELF/DEPT/DEPT_AND_CHILDREN` 和 `assignee_id` 过滤 | 需要改为按 `mailbox_id in visibleMailboxIds` 过滤 |
| 邮箱模型 | `mt_mailbox` 只有 `auto_reply_template_id`，无 `enterprise_id/sla_policy_id/assignment_rule_group_id/assignment_fallback_type` | 邮箱尚不能作为完整策略入口 |
| 自动回复 | `AutoReplyService` 仍按全局 `template_code=AUTO_REPLY` 查模板 | 需要改为优先读取邮箱绑定模板 |
| SLA | `SlaDeadlineService` 仍按启用默认策略选择 | 需要改为按邮箱绑定策略计算，未绑定不计算 |
| 分配规则 | `AssignmentRuleService` 仍是全局规则池，`DEFAULT` 仍生效 | 需要新增规则组，并让规则只通过邮箱绑定规则组生效 |
| 前端角色页 | `RoleManagePage` 仍展示和提交“默认数据范围” | 必须删除该区块和提交字段 |
| 前端用户页 | `UserManagePage` 仅维护账号、角色、部门、状态 | 必须增加可见企业/可见邮箱授权维护 |
| 前端邮箱页 | `MailboxManagePage` 没有企业、SLA、规则组、fallback 配置 | 必须改造为策略配置入口 |

### 1.3 计划修正

原文档中的阶段顺序需要收紧为“先门禁、再迁移、再业务链路、最后前端联调”：

1. `G0` 文档确认和基线冻结。
2. `P0` 角色数据范围退场，作为硬准入门禁。
3. `P1` 数据库迁移和模型基础。
4. `P2` 用户可见企业/邮箱授权核心。
5. `P3` 企业、邮箱、模板、SLA、规则组配置接口。
6. `P4` 建单、自动回复、SLA、自动分配核心链路。
7. `P5` 工单、客户、工作台、收发日志数据权限收口。
8. `P6` 前端页面和菜单调整。
9. `P7` 联调、迁移演练、上线门禁。

## 2. 总准入门禁

后续每个 AI 执行任务开始前都必须满足以下条件：

1. 读取本计划、两份 Obsidian 源文档、当前 `git status --short`。
2. 不回滚与当前任务无关的既有改动。
3. 新增数据库迁移从当前最后版本后追加；当前最后版本为 `V16__preserve_incoming_mail_original.sql`，下一版应为 `V17__enterprise_mailbox_permission_model.sql`。
4. Flyway 脚本必须兼容 MySQL 5.7，避免 MySQL 8 专属语法。
5. 每个阶段只修改本阶段文件，不提前实现后续阶段页面或接口。
6. 后端接口权限使用 `@RequirePermission` 或当前控制器既有权限校验方式，不能只靠前端隐藏。
7. 所有业务数据查询必须先套“用户可见邮箱”权限，再叠加企业、邮箱、我的、本部门、待分配等业务筛选。
8. 管理员可见全部邮箱，普通用户无授权时业务数据返回空集合，不回退到角色范围。
9. 每阶段完成后必须产出验收记录，包含改动文件、接口变更、测试命令、通过/失败结论、遗留问题。

业务确认门禁：

| 编号 | 待确认项 | 当前建议 |
|---|---|---|
| BIZ-01 | 菜单是否把“通知模板”改名为“自动回复模板” | 建议保留“通知模板”，页面内用模板类型筛选，避免后续分配通知、SLA 提醒无菜单归属 |
| BIZ-02 | 企业停用是否强制停用企业下邮箱 | 第一阶段不强制，只禁止新收信/新建单，页面展示风险 |
| BIZ-03 | 邮箱未绑定自动回复模板时是否发送 | 第一阶段不发送，记录配置缺失事件 |
| BIZ-04 | 邮箱未绑定 SLA 时是否计算 | 不计算，页面展示“未配置 SLA” |
| BIZ-05 | 非管理员用户是否必须至少一个授权 | 新建/编辑时建议强制，否则会出现空数据账号 |
| BIZ-06 | 企业授权是否包含未来新增邮箱 | 建议包含，并在用户授权区提示 |

## 3. 项目拆分

### G0：文档确认和基线冻结

目标：把模型边界、旧模型退场顺序、菜单文案、迁移口径确认清楚。

准入门禁：

1. 两份 Obsidian 文档已复核。
2. 当前仓库可读，`git status --short` 已记录。
3. 未开始写迁移脚本或业务代码。

实施动作：

1. 确认 `BIZ-01` 到 `BIZ-06`。
2. 检查 `docs/项目进度与阶段规划.md` 的当前进度游标，避免和既有阶段冲突。
3. 明确本次升级是否作为新阶段，例如 `P2-AUTH-MODEL-UPGRADE`。
4. 冻结接口命名和菜单文案。

输出：

1. 已确认的执行计划。
2. 本阶段验收记录：`docs/验收记录/MODEL-G0.md`。

验收标准：

1. 业务确认项都有结论。
2. 不产生代码变更。
3. 下一阶段只允许进入 `P0`。

回滚：

1. 无代码回滚。
2. 如业务结论变化，仅修订计划文档。

### P0：角色数据范围退场

目标：角色只保留菜单、按钮、接口权限，`mt_role_data_scope` 第一阶段保留表但不再读写。

准入门禁：

1. `G0` 已完成。
2. 确认前端角色页不再配置业务数据范围。
3. 确认登录接口是否继续兼容返回空 `dataScopes` 字段；建议短期返回空对象，减少前端旧缓存风险。

后端实施动作：

1. 修改 `PermissionService`，移除 `RoleDataScopeMapper` 依赖和 `resolveDataScopes` 读表逻辑。
2. `PermissionContext` 保留或临时兼容 `dataScopes=Map.of()`，但禁止再由角色解析业务数据范围。
3. 修改 `RoleManagementService`，角色权限保存只写 `mt_role_permission`，不删除也不插入 `mt_role_data_scope`。
4. 修改 `RolePermissionSaveRequest`、`RoleVO`，移除或兼容忽略 `dataScopes`。
5. 修改 `AuthService`、`CurrentUserVO`，登录和 `/auth/me` 不再表达角色数据范围；短期可返回空对象。
6. 更新 `PermissionServiceTest`、`RoleManagementServiceTest`、`AuthServiceTest`。

前端实施动作：

1. 删除 `RoleManagePage` 的“默认数据范围”区块。
2. 删除 `useRoleManagement` 中 `roleForm.dataScopes` 的构造、更新和提交。
3. 调整 `frontend/src/types/role.ts`、`frontend/src/api/roles.ts`、`frontend/src/constants/roles.ts`。
4. 用户列表不再展示 role profile 的“数据范围”，改为后续用户数据授权状态占位。
5. 移除 `frontend/src/constants/data-scopes.ts` 对角色页的依赖；如果后续用户授权说明不使用则删除。

输出：

1. 后端不再读写 `mt_role_data_scope`。
2. 前端角色页只展示菜单与操作权限。
3. 验收记录：`docs/验收记录/MODEL-P0.md`。

验收标准：

1. 新建/编辑角色成功。
2. 保存角色权限只提交 `permissionCodes`。
3. 登录后菜单和接口权限仍按角色生效。
4. `mt_role_data_scope` 表可以存在，但运行链路不访问。
5. 后端测试通过：`cd backend && mvn -q -Dtest=PermissionServiceTest,RoleManagementServiceTest,AuthServiceTest test`。
6. 前端构建通过：`cd frontend && pnpm build`。

回滚：

1. 回滚本阶段代码即可恢复旧角色数据范围。
2. 因未删除表，无数据库结构回滚压力。

### P1：数据库模型和历史数据迁移

目标：新增企业、规则组、用户数据授权表，并给邮箱、工单、客户、策略、模板、日历、日志补齐企业与策略字段。

准入门禁：

1. `P0` 完成且测试通过。
2. 确认默认企业名称，例如 `默认企业`。
3. 确认历史普通用户授权初始化口径。

后端实施动作：

1. 新增迁移 `backend/src/main/resources/db/migration/V17__enterprise_mailbox_permission_model.sql`。
2. 创建 `mt_enterprise`。
3. 创建 `mt_user_data_grant`，支持 `ENTERPRISE` 和 `MAILBOX` 两类授权。
4. 创建 `mt_assignment_rule_group`。
5. `mt_mailbox` 增加 `enterprise_id`、`sla_policy_id`、`assignment_rule_group_id`、`assignment_fallback_type`。
6. `mt_ticket` 增加 `enterprise_id`、`auto_reply_template_id`、`assignment_rule_group_id`、`assignment_rule_id`。
7. `mt_customer` 增加 `enterprise_id`，唯一约束从 `email` 调整为 `enterprise_id + email`。
8. `mt_notification_template` 增加 `enterprise_id`、`template_type`；保留 `template_code` 作为系统编码兼容字段。
9. `mt_sla_policy`、`mt_work_calendar` 增加 `enterprise_id`；保留 `is_default` 但后续工单不再依赖。
10. `mt_mail_fetch_log` 增加 `enterprise_id`；`mt_mail_send_log` 增加 `enterprise_id`、`template_id`、`template_type`。
11. 回填默认企业，历史邮箱、工单、客户、SLA、模板、日历、分配规则组归入默认企业。
12. 历史分配规则迁入默认规则组，但 `DEFAULT` 规则只保留数据，不默认绑定邮箱。
13. 给管理员不写全量授权也可见全部；给历史普通用户按默认企业初始化授权，避免上线后立即空列表。
14. 增加 Entity、Mapper、基础 VO/DTO。

输出：

1. V17 迁移脚本。
2. 新增企业、用户数据授权、规则组实体和 Mapper。
3. 修改相关实体字段。
4. 验收记录：`docs/验收记录/MODEL-P1.md`。

验收标准：

1. 全新库 Flyway 可从 V1 跑到 V17。
2. 历史库升级后所有历史邮箱、工单、客户都有 `enterprise_id`。
3. 历史工单的 `enterprise_id` 和来源邮箱所属企业一致。
4. 普通用户授权初始化符合确认口径。
5. MySQL 5.7 语法兼容。
6. 后端全量测试通过：`cd backend && mvn -q test`。

回滚：

1. 上线前必须备份数据库。
2. 如 V17 已执行且需回滚，恢复数据库备份；不要用手写反向 DDL 在生产库临时删字段。

### P2：用户可见企业/邮箱权限核心

目标：以用户授权替代角色数据范围，后端统一封装可见邮箱解析和断言。

准入门禁：

1. `P1` 完成。
2. `PermissionService` 已不读角色数据范围。
3. 可见邮箱算法已确认：企业授权展开启用邮箱 + 单独邮箱授权，管理员为全部启用邮箱。

后端实施动作：

1. 新增 `EnterpriseMailboxAccessService` 或重构 `DataScopeService`。
2. 实现 `isAdmin(principal)`，判断口径优先基于角色编码或 `PermissionContext.roles()`，不要依赖数据授权。
3. 实现 `resolveVisibleEnterpriseIds(principal)`。
4. 实现 `resolveVisibleMailboxIds(principal)`。
5. 实现 `assertMailboxVisible(principal, mailboxId)`。
6. 实现 `assertEnterpriseVisible(principal, enterpriseId)`。
7. 实现 `assertAssigneeCanAccessMailbox(assigneeId, mailboxId)`。
8. 对空授权普通用户返回空集合，查询层用 `in(empty)` 或 `eq(id, -1)` 防止查全表。
9. 加请求级缓存，避免一个请求多次解析同一用户授权。
10. 更新单元测试覆盖管理员、企业授权、邮箱授权、并集、空授权、停用授权、停用邮箱。

输出：

1. 可见邮箱权限服务。
2. 单元测试。
3. 验收记录：`docs/验收记录/MODEL-P2.md`。

验收标准：

1. 管理员可见全部启用邮箱。
2. 普通用户企业授权可见该企业启用邮箱。
3. 普通用户邮箱授权只可见指定启用邮箱。
4. 企业授权和邮箱授权取并集。
5. 普通用户空授权不能看到业务数据。
6. `assertAssigneeCanAccessMailbox` 能拦截跨邮箱错误分配。

回滚：

1. 若新权限服务异常，可临时回滚到 P1 代码状态。
2. 不恢复角色数据范围读写，除非业务明确要求整体回退。

### P3：企业和配置管理接口

目标：补齐企业管理、邮箱策略绑定、模板、SLA、日历、规则组的后端接口。

准入门禁：

1. `P2` 完成。
2. 新权限编码已确认并写入迁移或初始化脚本。
3. 所有配置接口明确是管理员/配置人员能力，不直接等同业务数据可见范围。

实施动作：

1. 新增 `EnterpriseController`、`EnterpriseService`，提供企业列表、选项、详情、新建、编辑、启停。
2. 修改 `MailboxController/MailboxService`：
   - 支持 `enterpriseId` 筛选。
   - 保存 `enterpriseId/slaPolicyId/assignmentRuleGroupId/assignmentFallbackType`。
   - 校验模板、SLA、规则组与邮箱企业一致。
   - 校验默认处理人可见该邮箱。
   - 提供当前用户可见邮箱 options。
3. 修改 `NotificationTemplateService`：
   - 支持 `enterpriseId/templateType`。
   - 页面选择按企业过滤。
   - 保留 `template_code` 兼容，但业务引用使用模板 ID。
4. 修改 `SlaPolicyService` 和 `WorkCalendarService`：
   - 支持企业归属和企业筛选。
   - 删除前校验邮箱引用。
   - `is_default` 不再作为建单选择依据。
5. 新增 `AssignmentRuleGroupService`。
6. 修改 `AssignmentRuleService`：
   - 规则按 `groupId` 管理。
   - 页面不再创建 `DEFAULT` 规则。
   - 规则处理人必须具备目标邮箱可见权限。
7. 新增或调整权限编码：
   - `menu:enterprise_config`
   - `menu:enterprises`
   - `enterprise:read/create/update/enable`
   - `assignment_rule_group:read/create/update/enable/delete`
   - 视实际接口补齐 API 权限。

输出：

1. 企业、邮箱策略、模板、SLA、日历、规则组接口。
2. 接口契约文档。
3. 验收记录：`docs/验收记录/MODEL-P3.md`。

验收标准：

1. 不能把企业 A 的模板/SLA/规则组绑定到企业 B 的邮箱。
2. 被邮箱引用的 SLA、模板、规则组不能无提示删除。
3. 普通用户 options 只返回可见企业/邮箱范围。
4. 管理员可维护全部配置。
5. 后端测试通过。

回滚：

1. 回滚接口和 Service 代码。
2. 已新增数据保留，不删除业务数据。

### P4：建单策略链路改造

目标：收信建单时按邮箱解析企业、客户、自动回复、SLA、分配规则组，并保存策略快照。

准入门禁：

1. `P3` 完成。
2. 默认企业和邮箱策略字段已可用。
3. 明确未绑定模板、SLA、规则组时的行为。

实施动作：

1. 修改 `TicketBizService.createTicket`：
   - 读取邮箱并校验邮箱启用、企业启用。
   - 写入 `enterprise_id/mailbox_id/customer_email`。
   - 客户按 `enterprise_id + email` 创建或更新。
   - 保存策略快照字段。
2. 修改 `AutoReplyService`：
   - 邮箱关闭自动回复则不发送。
   - 邮箱绑定模板且启用则发送并写入 `ticket.auto_reply_template_id`。
   - 未绑定模板第一阶段不发送，并记录事件或发件日志配置缺失。
3. 修改 `SlaDeadlineService`：
   - 增加 `calculateForNewTicket(mailboxId, startAt)` 或传入 policyId。
   - 只按 `mailbox.sla_policy_id` 计算。
   - 未绑定 SLA 返回 `none()`。
4. 修改 `AssignmentRuleService`：
   - 按邮箱绑定 `assignment_rule_group_id` 加载启用规则。
   - 规则组停用或无规则则待分配。
   - 命中规则后写入 `assignment_rule_group_id/assignment_rule_id`。
   - 未命中时仅在 `assignment_fallback_type=DEFAULT_ASSIGNEE` 时使用默认处理人。
   - 默认处理人和规则处理人都要校验可见目标邮箱。
5. 修改分配通知、发件日志，补 `enterprise_id/template_id/template_type`。
6. 更新 `TicketVO/TicketSummaryVO`，返回企业、邮箱、策略快照信息。

输出：

1. 建单核心链路改造。
2. 自动回复、SLA、分配规则组改造。
3. 验收记录：`docs/验收记录/MODEL-P4.md`。

验收标准：

1. 企业 A 邮箱 A1 绑定模板 T1、SLA S1、规则组 R1，建单后工单快照为 A/T1/S1/R1。
2. 邮箱未绑定模板不发自动回复。
3. 邮箱未绑定 SLA 不写截止时间。
4. 邮箱未绑定规则组或规则未命中，默认待分配。
5. 只有 fallback 明确为 `DEFAULT_ASSIGNEE` 时才分配默认处理人。
6. 跨可见邮箱分配被后端拒绝。

回滚：

1. 回滚建单链路代码。
2. 新字段保留，历史快照不清理。

### P5：业务查询数据权限收口

目标：工单、客户、工作台、收发记录全部统一按用户可见邮箱过滤。

准入门禁：

1. `P2` 权限核心和 `P4` 工单企业字段已完成。
2. 明确所有业务查询的邮箱来源字段。

实施动作：

1. 工单列表、统计、详情、回复、备注、转派、领取、附件全部先校验可见邮箱。
2. “全部工单、我的处理、本部门、待分配”只作为视图筛选：
   - 全部工单：可见邮箱内全部。
   - 我的处理：可见邮箱内 `assignee_id=currentUser`。
   - 本部门：可见邮箱内部门成员负责工单。
   - 待分配：可见邮箱内 `assignee_id is null`。
3. 客户管理按可见邮箱关联工单过滤，并支持企业/邮箱筛选。
4. 工作台统计按可见邮箱过滤，并支持企业/邮箱/时间范围。
5. 收件记录按 `mailbox_id in visibleMailboxIds` 过滤。
6. 发件记录按 `mailbox_id in visibleMailboxIds` 或工单关联邮箱过滤。
7. 附件下载、原始 EML 下载必须通过工单可见性校验。
8. 补充 Mapper 查询，避免在内存中过滤大数据。

输出：

1. 统一业务查询权限收口。
2. 单元测试和必要集成测试。
3. 验收记录：`docs/验收记录/MODEL-P5.md`。

验收标准：

1. 普通用户不能通过详情 ID 访问不可见邮箱工单。
2. 普通用户不能下载不可见工单附件或原始 EML。
3. 工作台数量和工单列表筛选结果一致。
4. 客户列表不会混入不可见邮箱客户。
5. 收发记录不泄露不可见邮箱。

回滚：

1. 回滚业务查询代码。
2. 不恢复角色数据范围。

### P6：前端模型权限升级

目标：前端完整表达新模型，并移除旧角色数据范围入口。

准入门禁：

1. `P3` 到 `P5` 后端接口已稳定。
2. 前端接口契约已更新。
3. 浏览器验收账号准备好：管理员、企业 A 用户、企业 B 用户、空授权用户。

前端实施动作：

1. 菜单调整：
   - 新增或调整“企业配置”分组。
   - 企业管理、邮箱配置、通知模板、SLA 策略、分配规则、工作日历归入企业配置。
   - 组织管理、用户管理、角色管理归入组织权限。
   - 收件记录、发件记录、编号规则归入系统运维。
2. 企业管理页：
   - 企业列表、启停、新建、编辑。
   - 展示邮箱数、工单数、启用状态。
3. 邮箱配置页：
   - 增加所属企业必填。
   - 增加自动回复模板、SLA 策略、分配规则组、未命中处理方式。
   - 默认处理人选择受当前邮箱可见权限约束。
   - 列表展示企业、策略绑定摘要、配置风险。
4. 通知模板页：
   - 增加企业、模板类型筛选。
   - 新建/编辑选择企业和类型。
   - 邮箱引用数展示。
5. SLA 策略和工作日历页：
   - 增加企业筛选和企业列。
   - SLA 策略选择企业内工作日历。
   - 移除“设置默认策略”作为建单依据的交互。
6. 分配规则页：
   - 调整为规则组 + 规则明细。
   - 规则组按企业过滤。
   - 测试规则支持邮箱或规则组。
   - 未命中明确显示“待分配”。
7. 用户管理页：
   - 增加可见企业、可见邮箱授权区。
   - 授权按企业分组展示邮箱。
   - 管理员展示“全部数据可见”，不要求逐条授权。
   - 普通用户保存时至少一个授权。
   - 用户列表展示授权摘要，替代旧角色数据范围。
8. 角色管理页：
   - 只展示菜单、按钮、接口权限。
   - 不展示和不提交 `dataScopes`。
9. 工单列表和详情：
   - 增加企业、邮箱筛选。
   - 增加企业、邮箱列。
   - 支持待分配、我的处理、本部门预设入口。
   - 详情右侧展示策略快照。
   - 转派处理人候选按当前工单邮箱过滤。
10. 客户、工作台、收发记录：
   - 增加企业、邮箱筛选。
   - 工作台图表点击跳转工单列表时带筛选参数。
   - 发件记录展示模板和发送类型。

输出：

1. 新增/调整 React 页面、hooks、types、api。
2. 更新菜单常量和权限判断。
3. 浏览器验收截图。
4. 验收记录：`docs/验收记录/MODEL-P6.md`。

验收标准：

1. 管理员能完成企业、邮箱策略、模板、SLA、规则组、用户授权配置。
2. 普通用户只看到授权企业/邮箱内的数据。
3. 角色页无数据范围配置。
4. 邮箱页能保存策略绑定并展示配置风险。
5. 工单详情可看到企业和策略快照。
6. `cd frontend && pnpm build` 通过。

回滚：

1. 回滚前端代码。
2. 后端接口不受影响。

### P7：联调、迁移演练和上线门禁

目标：验证多企业、多邮箱、多策略、多授权全链路，不带未验证结论上线。

准入门禁：

1. `P0` 到 `P6` 全部完成。
2. 测试库有可回滚备份。
3. 准备至少两个企业、三个邮箱、四个用户账号。

测试矩阵：

| 场景 | 预期 |
---|---|
| 企业 A / 邮箱 A1 绑定模板 T1、SLA S1、规则组 R1 | 建单命中 T1/S1/R1 并自动分配 |
| 企业 A / 邮箱 A2 绑定模板 T2、SLA S2、不绑定规则组 | 建单有 T2/S2，进入待分配 |
| 企业 B / 邮箱 B1 绑定模板 T3、不绑定 SLA、规则组 R2 | 建单有 T3，无 SLA，按 R2 分配 |
| 普通用户授权企业 A | 只能看到 A 下邮箱数据 |
| 普通用户仅授权 B1 邮箱 | 只能看到 B1 相关数据 |
| 普通用户空授权 | 工单、客户、工作台、收发记录为空 |
| 管理员 | 可见全部企业和邮箱 |
| 详情 ID 越权访问 | 后端返回 403 |
| 附件/原始 EML 越权下载 | 后端返回 403 |

构建和验证命令：

```bash
cd backend && mvn -q test
cd frontend && pnpm build
git diff --check
```

浏览器验证：

1. 登录管理员，配置企业、邮箱策略、模板、SLA、规则组、用户授权。
2. 登录普通用户，核对菜单、工单、客户、工作台、收发记录。
3. 截图保存到 `docs/验收记录/` 下对应目录。

输出：

1. 联调验收记录：`docs/验收记录/MODEL-P7.md`。
2. 数据库迁移演练记录。
3. 上线 SQL 和回滚说明。

上线门禁：

1. 测试库迁移成功。
2. 旧数据默认企业回填正确。
3. 后端测试通过。
4. 前端 build 通过。
5. 浏览器核心路径通过。
6. 权限越权测试通过。
7. 数据库已备份。

回滚：

1. 生产升级失败时恢复数据库备份。
2. 应用镜像回滚到上一版本。
3. 不在生产库临时手工删除 V17 字段。

## 4. AI 执行约束

后续让 AI 分阶段开发时，每次只给一个项目编号，例如：

```text
请执行 MailTrace 模型权限升级计划中的 P0：角色数据范围退场。
```

AI 必须遵守：

1. 先读本计划和对应源文档。
2. 只做当前项目，不提前实现下一项目。
3. 开始前检查 `git status --short`。
4. 每个项目完成后写验收记录。
5. 后端阶段至少跑目标测试；涉及共享权限、建单、查询时跑 `mvn -q test`。
6. 前端阶段必须跑 `pnpm build`，并在需要页面验收时启动 dev server 做浏览器截图。
7. 不删除 `mt_role_data_scope` 表，除非进入后续专门清理阶段。
8. 不把“我的、本部门、待分配”当权限边界，它们只是视图筛选。
9. 不以 dev server 启动成功代替前端 build 验收。
10. 不手动回滚用户已有改动。

## 5. 审核结论

### 5.1 可执行性审核

结论：可以执行，但必须按 `P0 -> P1 -> P2 -> P3 -> P4 -> P5 -> P6 -> P7` 顺序推进。

理由：

1. 当前旧角色数据范围仍在生产代码路径内，必须先退场。
2. 企业字段、邮箱策略字段、规则组字段依赖数据库迁移，不能由前端先行假接。
3. 用户可见邮箱权限是后续所有业务查询和转派校验的基础。
4. 建单链路涉及自动回复、SLA、分配规则、客户、日志，必须在配置接口之后改。
5. 前端页面依赖后端接口和 VO 稳定后再改，能减少返工。

### 5.2 风险审核

| 风险 | 等级 | 处理 |
|---|---|---|
| 角色数据范围和用户授权并存 | 高 | `P0` 硬门禁，后端不再读写 `mt_role_data_scope` |
| 历史普通用户上线后无数据 | 高 | `P1` 初始化默认企业授权，上线前抽样核对 |
| 空授权查询误查全表 | 高 | `P2` 空集合必须显式转为无结果条件 |
| 邮箱跨企业绑定模板/SLA/规则组 | 高 | `P3` 保存时强校验企业一致 |
| 处理人被分配到不可见邮箱工单 | 高 | `P2/P4/P5` 统一使用 `assertAssigneeCanAccessMailbox` |
| SLA 默认策略继续误作用 | 中 | `P4` 建单只按邮箱绑定策略计算 |
| DEFAULT 分配规则继续误命中 | 中 | `P4` 规则组模型不再执行全局 DEFAULT |
| 前端隐藏但接口未拦截 | 高 | 后端每个接口和下载都做权限校验 |
| MySQL 5.7 兼容问题 | 中 | V17 避免 8.0 语法，测试库先跑迁移 |

### 5.3 范围审核

本计划不包含：

1. 复杂 CRM 客户体系。
2. 企业自己的部门树或企业服务团队表。
3. 物理多租户隔离。
4. 企业级编号规则。
5. 邮箱组、处理人组、批量授权高级能力。
6. 删除 `mt_role_data_scope` 表的清理任务。

这些能力可以在新模型稳定后单独立项。

### 5.4 最终审核意见

采用“企业业务归属 + 全局运营组织 + 用户企业/邮箱授权”的轻量模型是当前阶段最稳妥的方案。下一步建议先执行 `G0`，确认 6 个业务门禁问题；确认后从 `P0` 开始实施，禁止跳过角色数据范围退场直接做数据库扩展。
