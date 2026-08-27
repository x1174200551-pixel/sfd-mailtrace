# MODEL-P6 上线 SQL 与回滚说明

## 1. 适用边界

本文只说明 V17→V19 的发布和回滚流程，不授权连接或修改任何远端数据库。执行前必须由谭总明确指定目标环境、数据库地址和维护窗口。

当前不可变更的远端 MySQL 5.7 不得作为默认目标；日常开发只使用本地配置和本地数据库，Nacos 保持关闭。

## 2. 上线 SQL 唯一来源

上线只允许由 Flyway 顺序执行以下文件：

1. `backend/src/main/resources/db/migration/V18__enterprise_mailbox_permission_model.sql`
2. `backend/src/main/resources/db/migration/V19__enterprise_config_permissions.sql`

禁止复制后手工拼接、改版本号或直接修改 `flyway_schema_history`。

## 3. 发布前检查

```sql
-- 确认当前版本；预期升级前最高版本为 17。
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;

-- 确认没有失败迁移。
SELECT COUNT(*) AS failed_count
FROM flyway_schema_history
WHERE success = 0;
```

操作者还必须完成：

1. 停止会写入目标库的旧应用实例。
2. 使用 `--single-transaction --routines --triggers` 生成升级前备份。
3. 记录备份文件大小和 SHA-256。
4. 在独立恢复库导入备份，并确认最高 Flyway 版本和业务表行数。

## 4. 升级后检查

```sql
-- 预期 V18、V19 均成功。
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('18', '19')
ORDER BY installed_rank;

-- 历史邮箱和业务数据必须有企业归属。
SELECT COUNT(*) AS mailbox_without_enterprise
FROM mt_mailbox
WHERE enterprise_id IS NULL AND is_deleted = 0;

SELECT COUNT(*) AS ticket_without_enterprise
FROM mt_ticket
WHERE enterprise_id IS NULL AND is_deleted = 0;
```

两个缺失归属计数都必须为 0，应用健康检查、管理员和普通用户权限矩阵通过后才能恢复流量。

## 5. 回滚

V18/V19 不提供手工 down SQL。需要回滚数据库时：

1. 立即停止新版本应用，避免产生 V19 数据。
2. 保留失败现场和日志。
3. 恢复升级前完整备份到经确认的目标库。
4. 核对恢复库最高版本为 V17，失败迁移为 0。
5. 回滚应用镜像到上一版本，再执行健康检查和登录冒烟。

禁止手工删除新增表、列、索引或权限数据，这种做法无法可靠恢复 V17 业务语义。
