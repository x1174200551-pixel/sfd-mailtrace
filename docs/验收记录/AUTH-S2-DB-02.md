# AUTH-S2-DB-02 初始化权限数据验收记录

| 项目 | 结论 |
|------|------|
| 验收时间 | 2026-07-28 15:50 CST |
| 任务范围 | 初始化 RBAC 角色、权限、用户角色关系和默认数据范围 |
| 任务状态 | ✅ 已完成 |
| 交付物 | `backend/src/main/resources/db/migration/V11__rbac_init_data.sql` |

## 1. 实施内容

新增 Flyway 迁移 `V11__rbac_init_data.sql`，完成以下初始化：

| 类型 | 内容 |
|------|------|
| 角色 | `ADMIN` 系统管理员、`AGENT` 客服处理人 |
| 菜单权限 | 17 个 `menu:*` 权限，覆盖当前 AppShell 菜单 |
| 动作权限 | 60 个动作权限，覆盖工单、附件、客户、用户、邮箱、日志、分配规则、SLA、工作日历、节假日、模板、编号规则 |
| 角色权限 | `ADMIN` 绑定全部 77 个权限；`AGENT` 绑定工作台、全部工单、客户只读和工单处理动作共 20 个权限 |
| 用户角色 | 根据未删除用户的旧 `mt_user.role_code` 回填 `mt_user_role` 主角色 |
| 数据范围 | `ADMIN = ALL`；`AGENT = SELF`，其中 `SELF` 维持当前口径：自己负责 + 未分配池 |

本任务不修改后端鉴权逻辑和前端菜单逻辑；服务层权限查询从 `AUTH-S2-BE-01` 开始接入。

## 2. 验证记录

| 验证项 | 命令/方式 | 结果 |
|--------|-----------|------|
| 临时库语法验证 | `V1__baseline.sql` + `V10__rbac_schema.sql` + `V11__rbac_init_data.sql` 导入临时库 | ✅ 通过 |
| Flyway 迁移 | 启动后端触发 Flyway | ✅ 从 schema version 10 迁移到 version 11 成功 |
| 迁移版本核查 | `SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;` | ✅ 最新版本为 `11 / rbac init data / success=1` |
| 权限数据核查 | 统计角色、权限、角色权限、用户角色、数据范围 | ✅ 2 个角色、17 个菜单权限、60 个动作权限、ADMIN 77 权限、AGENT 20 权限 |
| 用户角色回填 | `mt_user_role` 按 `mt_user.role_code` 回填 | ✅ ADMIN 1 个用户，AGENT 3 个未删除用户 |
| 后端测试 | `cd backend && mvn -q test` | ✅ 通过 |
| Diff 格式检查 | `git diff --check` | ✅ 通过 |

## 3. 注意事项

1. 迁移脚本沿用项目现有 `ON DUPLICATE KEY UPDATE ... VALUES(col)` 风格，MySQL 8 会提示 `VALUES function` 弃用警告，不影响迁移成功。
2. `AGENT` 的 `SELF` 数据范围在第一版仍代表“自己负责工单 + 未分配池”，与 AUTH-S1 已实现口径一致。
3. `MAILBOX`、`CUSTOMER_TAG` 数据范围未初始化，符合当前阶段边界。
