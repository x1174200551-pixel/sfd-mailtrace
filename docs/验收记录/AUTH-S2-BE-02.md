# AUTH-S2-BE-02 登录返回权限清单验收记录

| 项目 | 结论 |
|------|------|
| 验收时间 | 2026-07-28 16:06 CST |
| 任务范围 | `/auth/login`、`/auth/me` 返回 RBAC 权限清单 |
| 任务状态 | ✅ 已完成 |
| 交付物 | `CurrentUserVO`、`AuthService`、`AuthServiceTest` |

## 1. 实施内容

扩展当前用户返回结构：

| 字段 | 说明 |
|------|------|
| `roleCode` | 保留旧字段，兼容现有前端和旧业务判断 |
| `roles` | 返回当前用户 RBAC 角色编码集合 |
| `permissions` | 返回当前用户菜单和动作权限码集合 |
| `dataScopes` | 返回按资源聚合的数据范围，如 `TICKET -> ALL` |

登录与当前用户接口均接入 `PermissionService`：

| 接口 | 结果 |
|------|------|
| `POST /api/v1/auth/login` | 登录成功后 `data.user` 同时包含旧 `roleCode` 和新权限清单 |
| `GET /api/v1/auth/me` | JWT 鉴权后返回同样的用户权限结构 |

旧 5 参数 `CurrentUserVO` 构造函数保留，避免已有单测或调用方因新增字段直接破坏兼容。

## 2. 验证记录

| 验证项 | 命令/方式 | 结果 |
|--------|-----------|------|
| 目标单测 | `cd backend && mvn -q -Dtest=AuthServiceTest,PermissionServiceTest test` | ✅ 通过 |
| 后端全量测试 | `cd backend && mvn -q test` | ✅ 通过 |
| Spring 后端重启 | `launchctl submit -l mailtrace-backend-authqa -- ... mvn -q spring-boot:run` | ✅ 8080 启动成功，Flyway version 11 up to date |
| 登录接口冒烟 | `POST http://127.0.0.1:8080/api/v1/auth/login` | ✅ `roleCode=ADMIN`，`roles=[ADMIN]`，`permissions=77`，`dataScopes=CUSTOMER/DASHBOARD/TICKET -> ALL` |
| 当前用户接口冒烟 | `GET http://127.0.0.1:8080/api/v1/auth/me` | ✅ `roleCode=ADMIN`，`roles=[ADMIN]`，`permissions=77`，包含 `ticket:read` |
| Diff 格式检查 | `git diff --check` | ✅ 通过 |

## 3. 覆盖用例

1. 登录返回保留旧 `roleCode`，同时新增 `roles`、`permissions`、`dataScopes`。
2. `/auth/me` 复用同一套权限聚合逻辑，避免登录态和当前用户接口字段不一致。
3. 管理员账号可解析到 `ADMIN` 角色、77 个权限码，以及 `TICKET`、`CUSTOMER`、`DASHBOARD` 三类 `ALL` 数据范围。
4. `CurrentUserVO` 保持旧构造函数兼容，降低前端权限模型接入前的回归风险。
