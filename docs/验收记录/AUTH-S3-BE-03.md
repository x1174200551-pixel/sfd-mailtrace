# AUTH-S3-BE-03 主管角色启用 — 验收记录

| 字段 | 内容 |
|------|------|
| 任务编号 | AUTH-S3-BE-03 |
| 任务名称 | 主管角色启用 |
| 编写时间 | 2026-07-29 |
| 验收人 | AI（谭总确认） |
| 状态 | ✅ 已完成 |

## 交付物清单

| 交付物 | 路径 | 状态 |
|--------|------|------|
| 迁移文件 | `backend/src/main/resources/db/migration/V15__supervisor_role_init.sql` | ✅ |
| DataScopeService 适配 | `DataScopeService.java` 新增 `ROLE_SUPERVISOR` 常量 + `assertAgentOrAdmin` 放宽 | ✅ |
| DataScopeServiceTest 适配 | `DataScopeServiceTest.java` 更新错误消息断言 | ✅ |

## 验证结果

### 1. Flyway 迁移文件

**文件位置：** `backend/src/main/resources/db/migration/V15__supervisor_role_init.sql`

迁移内容：
- 新增 `SUPERVISOR` 角色（部门主管，系统内置，`sort_order=15`）
- 分配菜单权限：工作台、工单中心、客户、邮件管理（仅菜单入口）
- 分配动作权限：同 AGENT 全套工单/附件/客户操作 + `mailbox:read`、`mail_fetch_log:read`、`mail_send_log:read`
- 分配数据范围：`DEPT_AND_CHILDREN`（部门及下级）用于 TICKET、CUSTOMER、DASHBOARD

**验证命令：** `cd backend && mvn -q test`
**结果：** 测试通过 ✅ — Flyway 自动检测并验证了新的迁移文件

### 2. 后端代码编译

**验证命令：** `cd backend && mvn -q compile`
**结果：** 编译通过 ✅

### 3. 单元测试

| 测试类 | 命令 | 结果 |
|--------|------|------|
| DataScopeServiceTest | `mvn -q -Dtest=DataScopeServiceTest test` | ✅ 通过 |
| 全量测试 | `mvn -q test` | ✅ 通过 |

### 4. 代码检查

**验证命令：** `git diff --check`
**结果：** 无空格/缩进/合并冲突问题 ✅

## 变更摘要

### 新增文件

| 文件 | 说明 |
|------|------|
| `backend/src/main/resources/db/migration/V15__supervisor_role_init.sql` | Supervisor 角色迁移 SQL |

### 修改文件

| 文件 | 说明 |
|------|------|
| `DataScopeService.java` | 新增 `ROLE_SUPERVISOR` 常量；`assertAgentOrAdmin` 回退路径允许 SUPERVISOR |
| `DataScopeServiceTest.java` | 更新错误消息断言以匹配新文案 |

## 注意事项

1. **数据范围解析**：SUPERVISOR 角色的 `DEPT_AND_CHILDREN` 数据范围已在 `mt_role_data_scope` 中配置，但 `DataScopeService.applyTicketScope()` 当前仅处理 `ALL` 和 `SELF` 两种范围。`DEPT_AND_CHILDREN` 的运行时解析将由后续 **AUTH-S3-BE-04** 任务实现。
2. **下线**：现有 ADMIN 和 AGENT 角色的权限和数据范围不受本次变更影响。
3. **升级**：旧用户通过 `role_code` 迁移后已有 `mt_user_role` 关联，不会自动获得 SUPERVISOR 角色 — 需管理员手动为用户分配。
