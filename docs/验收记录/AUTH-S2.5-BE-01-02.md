# AUTH-S2.5-BE-01/02 角色管理接口与角色权限配置接口验收记录

| 字段 | 内容 |
|------|------|
| 验收时间 | 2026-07-28 17:14 CST |
| 当前状态 | ✅ 已完成 |
| 关联原型 | `docs/原型设计/16、邮件工单系统-角色配置管理原型.html` |
| 主要代码 | `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/role/RoleManagementService.java`；`backend/src/main/java/com/ntn/fziot/mailtrace/interfaces/api/role/RoleController.java` |

## 1. 实现范围

1. 新增角色管理接口，支持角色列表、新建、编辑、启停。
2. 新增权限树接口，返回菜单和操作权限的层级结构。
3. 新增角色权限配置接口，支持保存权限清单和默认数据范围。
4. 新增 `V12__rbac_role_management_permissions.sql`，初始化角色管理菜单和操作权限，并默认授权给管理员。
5. 内置角色只读保护：管理员、客服处理人不可通过角色管理接口编辑、停用或重新配置权限。
6. 当前阶段数据范围只支持 `TICKET`、`CUSTOMER`、`DASHBOARD` 与 `ALL`、`SELF`，不支持指定邮箱和客户标签。

## 2. Service 层步骤

1. `listRoles`：校验 `role:read`，构建查询条件，查询角色主表，补齐权限清单、数据范围、关联用户数，返回统计摘要。
2. `listPermissionTree`：校验 `role:read`，查询启用权限，按 `parentId` 组装权限树。
3. `createRole`：校验 `role:create`，校验名称和编码，写入非内置角色，记录操作日志，返回详情。
4. `updateRole`：校验 `role:update`，查询角色，拦截内置角色，更新名称、说明和启用状态，记录操作日志。
5. `updateEnabled`：校验 `role:enable`，查询角色，拦截内置角色，更新启停状态，记录操作日志。
6. `saveRolePermissions`：校验 `role:permission_update`，查询角色，拦截内置角色，校验权限和数据范围白名单，替换角色权限关系和数据范围关系，记录操作日志。

## 3. 验证记录

| 检查项 | 结果 |
|--------|------|
| 定向单测 | `mvn -q -Dtest=RoleManagementServiceTest test` 通过 |
| 后端全量测试 | `mvn -q test` 通过 |
| Flyway 迁移 | 临时端口启动后 V12 成功应用，`flyway_schema_history` 当前 version 12 |
| 权限数据核查 | `menu:roles`、`role:read/create/update/enable/permission_update` 均已写入 |
| 管理员授权核查 | ADMIN 角色拥有 6 个角色管理权限 |
| 接口冒烟 | 管理员登录后可访问角色列表和权限树；处理人访问角色列表返回 40302 |

## 4. 边界说明

1. 本阶段不提前实现用户多角色分配，用户分配入口仍由 `AUTH-S2.5-BE-03` 处理。
2. 本阶段没有提供删除角色接口，避免和当前原型“内置保护、自定义启停”的第一版边界冲突。
3. 关系表替换使用物理删除再插入，避免逻辑删除与唯一索引组合导致后续恢复同一权限时报唯一键冲突。
