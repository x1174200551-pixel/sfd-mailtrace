# MODEL-P0-EXPAND 数据库扩展和历史回填验收记录

| 项目 | 结论 |
|------|------|
| 任务编号 | MODEL-P0-EXPAND |
| 当前状态 | ✅ 已完成 |
| 开始时间 | 2026-08-26 15:15 CST |
| 完成时间 | 2026-08-26 15:28 CST |
| 前置门禁 | MODEL-G0 ✅ |
| 下一阶段 | MODEL-P1-SHADOW |

## 实施结果

### 数据库与模型

1. 新增 `V18__enterprise_mailbox_permission_model.sql`，创建 `mt_enterprise`、`mt_user_data_grant`、`mt_assignment_rule_group`。
2. 邮箱、工单、客户、模板、SLA、工作日历、分配规则和收发信日志已增加企业或策略字段。
3. 历史业务数据归入“默认企业”，历史分配规则归入“默认规则组”。
4. 历史普通用户初始化默认企业授权，管理员保持隐式全量可见，不生成全量授权行。
5. 新增企业、用户数据授权、分配规则组 Entity、Mapper 和基础 VO；现有相关 Entity 已同步字段。

### 验证证据

1. 修改前、修改后均执行 `cd backend && mvn -q test`，结果通过。
2. 本地历史库由 Flyway V17 升级至 V18 成功，`flyway_schema_history` 记录 `version=18`、`success=1`。
3. 历史库核对：邮箱、客户、工单、模板、SLA、工作日历、拉信日志和发信日志的 `enterprise_id` 空值数均为 0；历史分配规则 `group_id` 空值数为 0。
4. 默认企业 1 条、默认规则组 1 条；3 个历史普通用户均生成 `ENTERPRISE` 授权，未授权普通用户数为 0。
5. 创建专用临时库，从空库完整执行 Flyway V1→V18，共 18 个迁移全部成功；核对后已删除临时库。
6. V18 不使用 CTE、窗口函数、`CHECK` 等 MySQL 8 专属能力，按 MySQL 5.7 语法约束实现；本机 MySQL 8 完成实际执行验证。
7. 未修改 `PermissionService`、`DataScopeService`、角色保存逻辑或前端权限页面；V18 不读写 `mt_role_data_scope`，现有 15 条角色数据范围记录保留。
8. 当前代码使用本地配置、本地数据库、Nacos 关闭，在统一端口 `8080` 启动成功；`GET /api/v1/system/health` 返回 HTTP 200 和 `UP`。

## 验收结论

`MODEL-P0-EXPAND` 验收通过，允许进入 `MODEL-P1-SHADOW`。新授权表在 P1 仅用于影子计算，仍不得替换现有生产查询条件。

## 阶段边界

本阶段不得修改 `PermissionService`、`DataScopeService`、角色数据范围保存逻辑和前端角色数据范围页面；不得让新授权参与生产查询。
