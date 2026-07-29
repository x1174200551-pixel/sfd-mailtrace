# AUTH-S3-BE-04 TEAM/DEPT 数据范围 — 验收记录

| 字段 | 内容 |
|------|------|
| 任务编号 | AUTH-S3-BE-04 |
| 任务名称 | TEAM/DEPT 数据范围 |
| 编写时间 | 2026-07-29 |
| 验收人 | AI |
| 状态 | ✅ 已完成 |

## 交付物清单

| 交付物 | 路径 | 状态 |
|--------|------|------|
| DepartmentMemberService | `DepartmentMemberService.java` | ✅ 新建 |
| DataScopeService 更新 | `DataScopeService.java` | ✅ 新增 DEPT/DEPT_AND_CHILDREN 分支 |
| CustomerMapper 更新 | `CustomerMapper.java` | ✅ 新增 scopeAssigneeIds 参数 |
| CustomerReadonlyService 更新 | `CustomerReadonlyService.java` | ✅ 使用 scopeAssigneeIds |
| DataScopeServiceTest 更新 | `DataScopeServiceTest.java` | ✅ 新增部门范围测试 |
| CustomerReadonlyServiceTest 更新 | `CustomerReadonlyServiceTest.java` | ✅ 适配新签名 |

## 验证结果

### 1. 单元测试

**验证命令：** `cd backend && mvn -q test`
**结果：** 全部通过 ✅

| 测试类 | 结果 |
|--------|------|
| DataScopeServiceTest（含 3 个新增部门范围测试） | ✅ |
| CustomerReadonlyServiceTest | ✅ |
| 其他已有测试 | ✅ 无回归 |

### 2. 代码检查

**验证命令：** `git diff --check`
**结果：** 无空格/缩进/合并冲突问题 ✅

## 变更摘要

### 新增文件

| 文件 | 说明 |
|------|------|
| `DepartmentMemberService.java` | 部门成员解析服务：`resolveDeptAndChildrenMemberIds()` 和 `resolveDeptMemberIds()` |

### 修改文件

| 文件 | 说明 |
|------|------|
| `DataScopeService.java` | 新增 `SCOPE_DEPT`/`SCOPE_DEPT_AND_CHILDREN` 常量；注入 `DepartmentMemberService`；`applyTicketScope`/`assertTicketVisible`/`assertTicketOperable` 均按 ALL → DEPT_AND_CHILDREN → DEPT → SELF 宽度顺序检查；新增 `resolveTicketScopeUserIds()` 供客户模块使用 |
| `CustomerMapper.java` | 3 个 SQL 查询方法新增 `scopeAssigneeIds` 参数，非 ADMIN 时按 IN 条件过滤 |
| `CustomerReadonlyService.java` | 使用 `dataScopeService.resolveTicketScopeUserIds()` 获取范围用户 ID 并传入 Mapper |
| `DataScopeServiceTest.java` | 新增 3 个测试：dept 列表范围、dept 可见性拒绝跨部门、dept 可操作性允许部门内 |
| `CustomerReadonlyServiceTest.java` | 所有 mapper mock 调用增加 scopeAssigneeIds 参数 |

## 数据范围解析逻辑

### applyTicketScope（工单列表）

| 范围 | SQL WHERE 条件 |
|------|---------------|
| ALL | 不限制 |
| DEPT_AND_CHILDREN | `assignee_id IN (部门及下级成员 ID) OR assignee_id IS NULL` |
| DEPT | `assignee_id IN (部门成员 ID) OR assignee_id IS NULL` |
| SELF | `assignee_id = 当前用户 OR assignee_id IS NULL` |

### assertTicketVisible（工单详情可见性）

| 范围 | 规则 |
|------|------|
| ALL | 可见 |
| DEPT_AND_CHILDREN / DEPT | assignee 在部门成员范围内（含未分配）可见 |
| SELF | 自己负责或未分配可见 |

### assertTicketOperable（工单操作）

| 范围 | 规则 |
|------|------|
| ALL | 可操作 |
| DEPT_AND_CHILDREN / DEPT | assignee 在部门成员范围内（含未分配）可操作 |
| SELF | 只能操作自己负责的工单，未分配不可操作 |
