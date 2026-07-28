# AUTH-S2-BE-03/04 管理接口与工单动作权限迁移验收记录

| 项目 | 结论 |
|------|------|
| 验收时间 | 2026-07-28 16:19 CST |
| 任务范围 | 管理接口和工单动作从硬编码角色迁移到权限码判断 |
| 任务状态 | ✅ 已完成 |
| 交付物 | `PermissionService.assertPermission`、管理模块权限断言、工单动作权限断言、相关单测更新 |

## 1. 实施内容

新增统一权限断言：

| 能力 | 说明 |
|------|------|
| `PermissionService.assertPermission` | 基于当前登录用户 RBAC 权限码判断接口动作，失败返回 40302 |
| `DataScopeService` | 生产环境改为读取 `mt_role_data_scope` 的 `TICKET:ALL/SELF`，不再只依赖旧角色枚举 |

管理接口按权限码迁移：

| 模块 | 权限码 |
|------|--------|
| 用户管理 | `user:read/create/update/enable/reset_password` |
| 邮箱配置 | `mailbox:read/create/update/enable/delete/test_connection` |
| 分配规则 | `assignment_rule:read/create/update/enable/sort/test_match/delete` |
| SLA 策略 | `sla_policy:read/create/update/enable/default/delete` |
| 工作日历 | `work_calendar:read/create/update/default/delete` |
| 节假日 | `holiday:read/import/create/update/delete` |
| 通知模板 | `notification_template:read/create/update/preview` |
| 编号规则 | `ticket_number_rule:read/preview/update` |
| 收发日志 | `mail_fetch_log:read`、`mail_send_log:read`、`mail_send:test/retry` |

工单动作按权限码迁移：

| 动作 | 权限码 |
|------|--------|
| 工单列表/详情/统计 | `ticket:read` |
| 领取未分配工单 | `ticket:claim` |
| 回复客户 | `ticket:reply` |
| 内部备注 | `ticket:note` |
| 转派工单 | `ticket:assign` |
| 关闭工单 | `ticket:close` |
| 手动变更状态 | `ticket:update_status` |
| 变更优先级 | `ticket:update_priority` |
| 编辑备注字段 | `ticket:update_remark` |
| 附件 | `ticket_attachment:read/upload/download/delete` |

`closeTicket` 与 `updateStatus` 的权限入口已拆开，避免“关闭工单”被误绑定到“任意状态变更”权限。

## 2. 验证记录

| 验证项 | 命令/方式 | 结果 |
|--------|-----------|------|
| 受影响目标测试 | `cd backend && mvn -q -Dtest=AssignmentRuleServiceTest,SlaPolicyServiceTest,WorkCalendarServiceTest,HolidayServiceTest,NationalHolidayPresetServiceTest,CustomerReadonlyServiceTest,DashboardServiceTest,TicketAttachmentServiceTest,TicketBizServiceTest,DataScopeServiceTest test` | ✅ 通过 |
| 后端全量测试 | `cd backend && mvn -q test` | ✅ 通过 |
| Spring 后端重启 | `launchctl submit -l mailtrace-backend-authqa -- ... mvn -q spring-boot:run` | ✅ 8080 启动成功，Flyway version 11 up to date |
| ADMIN 管理接口冒烟 | `GET /api/v1/users` | ✅ `code=0`，用户总数 4 |
| AGENT 管理接口冒烟 | `GET /api/v1/users` | ✅ `code=40302`，`无权查看用户管理` |
| AGENT SLA 管理接口冒烟 | `GET /api/v1/sla-policies` | ✅ `code=40302`，`无权查看 SLA 策略` |
| AGENT 工单列表冒烟 | `GET /api/v1/tickets` | ✅ `code=0`，按数据范围返回 1 条 |
| Diff 格式检查 | `git diff --check` | ✅ 通过 |

## 3. 覆盖用例

1. ADMIN 仍拥有全部管理接口权限。
2. AGENT 无管理菜单对应动作权限时，用户管理和 SLA 策略接口返回 40302。
3. AGENT 保留工单、附件、客户、工作台等第一版操作权限，并继续叠加 `SELF` 数据范围。
4. 工单关闭与普通状态变更使用不同权限码，便于后续角色精细授权。
