# AUTH-S2-QA-01 RBAC 回归验收记录

| 项目 | 结论 |
|------|------|
| 验收时间 | 2026-07-28 16:55 CST |
| 验收范围 | 菜单、按钮、接口动作与 RBAC 权限清单一致性 |
| 当前状态 | ✅ 通过 |
| 管理员截图 | `docs/验收记录/AUTH-S2-QA-01-admin-user-menu.png` |
| 处理人截图 | `docs/验收记录/AUTH-S2-QA-01-agent-menu.png` |

## 1. 接口权限矩阵

| 账号 | 验证项 | 结果 |
|------|--------|------|
| 管理员 `admin` | 登录返回角色 | ✅ `ADMIN` |
| 管理员 `admin` | 权限数量 | ✅ 77 |
| 管理员 `admin` | 数据范围 | ✅ `TICKET/CUSTOMER/DASHBOARD = ALL` |
| 管理员 `admin` | 用户列表 | ✅ 200 / `code=0` / 4 条 |
| 管理员 `admin` | SLA 策略 | ✅ 200 / `code=0` / 1 条 |
| 管理员 `admin` | 工单列表 | ✅ 200 / `code=0` / 10 条 |
| 处理人 `agentperm72230` | 登录返回角色 | ✅ `AGENT` |
| 处理人 `agentperm72230` | 权限数量 | ✅ 20 |
| 处理人 `agentperm72230` | 数据范围 | ✅ `TICKET/CUSTOMER/DASHBOARD = SELF` |
| 处理人 `agentperm72230` | 用户列表 | ✅ 403 / `code=40302` / 无权查看用户管理 |
| 处理人 `agentperm72230` | SLA 策略 | ✅ 403 / `code=40302` / 无权查看 SLA 策略 |
| 处理人 `agentperm72230` | 工单列表 | ✅ 200 / `code=0` / 1 条 |
| 处理人 `agentperm72230` | 工作台统计 | ✅ 200 / `code=0` / 当前范围 1 条 |
| 处理人 `agentperm72230` | 客户列表 | ✅ 200 / `code=0` / 1 条 |

## 2. 前端菜单与入口

1. 管理员侧边栏展示工作空间、工单中心、邮件管理、SLA 管理、系统管理。
2. 管理员用户管理页展示用户列表、角色、菜单范围、数据范围和用户操作按钮。
3. 处理人侧边栏仅展示工作台、全部工单、客户管理。
4. 处理人不展示邮件管理、SLA 管理、系统管理。
5. 处理人工作台不再展示邮箱配置、收件记录、发件记录快捷入口。

## 3. 数据关系核查

只读 SQL 核查 `mt_user.role_code` 与 `mt_user_role` 主角色一致：

| 账号 | 用户角色字段 | RBAC 主角色 | 是否主角色 |
|------|--------------|-------------|------------|
| admin | ADMIN | ADMIN | 1 |
| agent140350 | AGENT | AGENT | 1 |
| agentperm72230 | AGENT | AGENT | 1 |
| tplagent58619 | AGENT | AGENT | 1 |

## 4. 自动化与构建

| 验证项 | 命令/方式 | 结果 |
|--------|-----------|------|
| 后端全量测试 | `cd backend && mvn -q test` | ✅ 通过 |
| 前端构建 | `cd frontend && pnpm build` | ✅ 通过 |
| 前端 lint | `cd frontend && pnpm lint` | ✅ 通过，保留既有 hook 依赖 warning |
| 差异检查 | `git diff --check` | ✅ 通过 |
| 临时截图入口 | `frontend/public/rbac-qa-seed.html` | ✅ 已删除 |

## 5. QA 中发现并修复的问题

1. 处理人菜单已隐藏邮件管理，但工作台仍展示邮箱运行快捷入口。
2. 已修复为工作台快捷入口按 `menu:mailboxes`、`menu:mail_fetch_logs`、`menu:mail_send_logs` 权限过滤。
3. 处理人截图复验确认邮箱运行入口已隐藏。

## 6. 后续动作

1. `AUTH-S2` 当前管理员/处理人 RBAC 闭环完成。
2. 下一步按计划进入 `AUTH-S2.5-PG-01`，补后台自建角色的角色配置管理原型。
