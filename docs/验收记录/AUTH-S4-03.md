# AUTH-S4-03 操作审计补强

| 字段 | 内容 |
|------|------|
| 任务编号 | AUTH-S4-03 |
| 任务名称 | 操作审计补强 |
| 阶段 | AUTH-S4 权限验收与硬化 |
| 编写时间 | 2026-07-29 |

## 1. 审计日志现状

### 已有基础设施
- `mt_operation_log` 表（V1 基线已建）
- `OperationLogEntity` + `OperationLogMapper`（已存在）

### 关键操作已有日志覆盖

| 操作 | Service + 方法 | ActionCode | 行号 |
|------|---------------|-----------|------|
| 新建角色 | `RoleManagementService.createRole()` | `CREATE` | ✅ |
| 编辑角色 | `RoleManagementService.updateRole()` | `UPDATE` | ✅ |
| 启停角色 | `RoleManagementService.updateEnabled()` | `ENABLE`/`DISABLE` | ✅ |
| 配置角色权限 | `RoleManagementService.saveRolePermissions()` | `PERMISSION_UPDATE` | ✅ |
| 新建部门 | `DepartmentService.createDepartment()` | `CREATE` | ✅ |
| 编辑部门 | `DepartmentService.updateDepartment()` | `UPDATE` | ✅ |
| 启停部门 | `DepartmentService.updateEnabled()` | `ENABLE`/`DISABLE` | ✅ |
| 新建用户 | `UserService.createUser()` | `CREATE` | ✅ |
| 编辑用户 | `UserService.updateUser()` | `UPDATE` | ✅ |
| 启停用户 | `UserService.updateEnabled()` | `ENABLE`/`DISABLE` | ✅ |
| 重置密码 | `UserService.resetPassword()` | `RESET_PASSWORD` | ✅ |

---

## 2. 本次重构内容

### 2.1 新增 `OperationLogService`（集中审计服务）

**文件：** `backend/.../bizservice/security/OperationLogService.java`

- 统一入口：`record(principal, moduleCode, actionCode, bizId, content)`
- 自动捕获 `requestUri` 和 `requestIp`（通过 `RequestContextHolder`）
- IP 支持 `X-Forwarded-For` / `X-Real-IP` / `RemoteAddr` 三级回退
- 非 Web 上下文（定时任务）优雅降级

### 2.2 迁移 3 个核心 Service 到集中服务

| Service | 原有代码 | 重构后 |
|---------|---------|--------|
| `RoleManagementService` | 私有 `recordLog()` + `operationLogMapper.insert()` | 注入 `OperationLogService` |
| `DepartmentService` | 同上 | 同上 |
| `UserService` | 同上 | 同上 |

### 2.3 补全字段

原 `recordLog` 方法未设置 `requestUri` 和 `requestIp` 字段，新 `OperationLogService` 自动补全。

---

## 3. 验证结果

| 检查项 | 结果 |
|--------|------|
| 所有审计日志点保留 | ✅ 功能不退化 |
| 新增 `OperationLogService` 编译通过 | ✅ |
| 3 个 Service 迁移完成 | ✅ |
| 后端全量测试 | ✅ 163 测试通过，0 失败，0 错误 |
| 代码无新警告 | ✅ |

---

## 4. 后续建议

| 建议 | 说明 | 优先级 |
|------|------|--------|
| 迁移其余 8 个 Service | MailboxService、SlaPolicyService、AssignmentRuleService、HolidayService、WorkCalendarService、NotificationTemplateService、TicketNumberRuleService、TicketBizService 的 `recordLog()` 仍为重复代码 | 🟡 低 |
| 操作日志查询页 | 当前无前台页面查看历史操作日志 | 🟢 未来 |

---

*验收人：* （自动）
*验证日期：* 2026-07-29
