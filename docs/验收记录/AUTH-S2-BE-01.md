# AUTH-S2-BE-01 权限查询服务验收记录

| 项目 | 结论 |
|------|------|
| 验收时间 | 2026-07-28 15:57 CST |
| 任务范围 | 新增 RBAC 权限查询 Service |
| 任务状态 | ✅ 已完成 |
| 交付物 | `PermissionService`、RBAC Entity/Mapper、`PermissionServiceTest` |

## 1. 实施内容

新增 RBAC 表对应的 Entity 和 Mapper：

| 表 | Entity | Mapper |
|----|--------|--------|
| `mt_role` | `RoleEntity` | `RoleMapper` |
| `mt_permission` | `PermissionEntity` | `PermissionMapper` |
| `mt_role_permission` | `RolePermissionEntity` | `RolePermissionMapper` |
| `mt_user_role` | `UserRoleEntity` | `UserRoleMapper` |
| `mt_role_data_scope` | `RoleDataScopeEntity` | `RoleDataScopeMapper` |

新增 `PermissionService`：

| 能力 | 说明 |
|------|------|
| 当前用户权限聚合 | 基于 `CurrentUserPrincipal` 查询 roles、permissions、dataScopes |
| 用户角色解析 | 优先使用 `mt_user_role`，为空时回退旧 `role_code` |
| 权限码解析 | 通过 `mt_role_permission` 聚合启用权限码 |
| 数据范围解析 | 按 `resourceType -> scopeCode` 聚合角色数据范围 |
| 便捷判断 | 支持 `hasRole`、`hasPermission`、`hasDataScope` |

本任务不修改登录返回结构，不接入前端权限模型；这些放到后续 `AUTH-S2-BE-02` 和 `AUTH-S2-FE-01`。

## 2. 验证记录

| 验证项 | 命令/方式 | 结果 |
|--------|-----------|------|
| 权限服务单测 | `cd backend && mvn -q -Dtest=PermissionServiceTest test` | ✅ 通过 |
| 后端全量测试 | `cd backend && mvn -q test` | ✅ 通过 |
| Spring 容器启动 | `cd backend && mvn -q spring-boot:run` | ✅ 启动成功，Flyway schema version 11 up to date |
| Diff 格式检查 | `git diff --check` | ✅ 通过 |

## 3. 覆盖用例

1. 用户存在 `mt_user_role` 时，按用户角色关系解析权限和数据范围。
2. 旧用户缺少 `mt_user_role` 时，回退 `mt_user.role_code` 查询角色。
3. 权限码保持 `ticket:read`、`menu:tickets` 等原始格式，不做大写转换。
4. 数据范围支持大小写无关判断，如 `ticket/self` 可匹配 `TICKET/SELF`。
5. 未登录上下文查询权限返回业务异常。
