# AUTH-S2-DB-01 RBAC 表结构迁移验收记录

| 项目 | 结论 |
|------|------|
| 验收时间 | 2026-07-28 15:45 CST |
| 任务范围 | 新增 RBAC 基础表结构 |
| 任务状态 | ✅ 已完成 |
| 交付物 | `backend/src/main/resources/db/migration/V10__rbac_schema.sql` |

## 1. 实施内容

新增 Flyway 迁移 `V10__rbac_schema.sql`，创建以下表：

| 表名 | 用途 |
|------|------|
| `mt_role` | 角色定义 |
| `mt_permission` | 菜单、动作、接口权限定义 |
| `mt_role_permission` | 角色权限关系 |
| `mt_user_role` | 用户角色关系 |
| `mt_role_data_scope` | 角色数据范围关系 |

本任务只负责表结构迁移，不初始化 `ADMIN`、`AGENT`、权限码和默认数据范围；初始化数据放到 `AUTH-S2-DB-02` 单独执行。

## 2. 验证记录

| 验证项 | 命令/方式 | 结果 |
|--------|-----------|------|
| Flyway 迁移 | 启动后端触发 Flyway | ✅ 从 schema version 9 迁移到 version 10 成功 |
| 迁移版本核查 | `docker exec mysql mysql -uroot -proot123 -D mailtrace -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;"` | ✅ 最新版本为 `10 / rbac schema / success=1` |
| 表存在核查 | `SHOW TABLES LIKE 'mt_role'` 等 5 张表 | ✅ 五张 RBAC 表均存在 |
| 后端测试 | `cd backend && mvn -q test` | ✅ 通过 |
| Diff 格式检查 | `git diff --check` | ✅ 通过 |

## 3. 注意事项

1. 本迁移沿用项目现有 `TINYINT(1)` 布尔字段风格，MySQL 8 启动迁移时会提示整数显示宽度弃用警告，不影响迁移成功。
2. 当前仍保留旧的 `mt_user.role_code` 兼容字段，后续 `AUTH-S2-BE-01`/`AUTH-S2-BE-02` 会逐步迁移到角色权限查询。
3. `MAILBOX`、`CUSTOMER_TAG` 数据范围不在本阶段范围内。
