# AUTH-S3-DB-01 组织表结构迁移验收记录

## 任务范围

新增轻量组织架构数据库基础：

1. 新增部门表 `mt_department`。
2. 新增用户部门关系表 `mt_user_department`。
3. 初始化「默认部门」。
4. 将历史未删除用户回填到默认部门，并标记为主部门。
5. 补充后续 Service 层可直接使用的实体和 Mapper。

## 代码交付

| 类型 | 文件 |
|------|------|
| Flyway 迁移 | `backend/src/main/resources/db/migration/V13__organization_schema.sql` |
| 部门实体 | `backend/src/main/java/com/ntn/fziot/mailtrace/repox/mysql/entity/DepartmentEntity.java` |
| 用户部门关系实体 | `backend/src/main/java/com/ntn/fziot/mailtrace/repox/mysql/entity/UserDepartmentEntity.java` |
| 部门 Mapper | `backend/src/main/java/com/ntn/fziot/mailtrace/repox/mysql/mapper/DepartmentMapper.java` |
| 用户部门关系 Mapper | `backend/src/main/java/com/ntn/fziot/mailtrace/repox/mysql/mapper/UserDepartmentMapper.java` |

## 表结构口径

### `mt_department`

支持部门树、负责人、启停、排序和路径字段：

1. `parent_id`：父部门。
2. `dept_code` / `dept_name`：部门编码和名称。
3. `leader_user_id`：部门负责人。
4. `dept_path`：预留下级部门范围查询路径。
5. `is_enabled` / `sort_order`：启停和排序。

### `mt_user_department`

支持用户多部门关系，并用 `is_primary` 标识主部门。第一版页面先只维护主部门，后续如需多部门不再需要改主表结构。

## 验证记录

| 验证项 | 结果 |
|--------|------|
| `cd backend && mvn -q test` | 通过 |
| `git diff --check` | 通过 |

## 边界说明

本任务只完成组织架构 DB 基础，不包含部门 Service、Controller、前端组织管理页面，也不提前启用 `SUPERVISOR` 角色或 `DEPT` / `DEPT_AND_CHILDREN` 数据范围计算逻辑。上述内容按计划进入后续任务：

1. `AUTH-S3-BE-01` 部门基础服务。
2. `AUTH-S3-BE-02` 用户部门关系。
3. `AUTH-S3-BE-03` 主管角色启用。
4. `AUTH-S3-BE-04` 部门数据范围解析。

## 结论

`AUTH-S3-DB-01` 技术验证通过，可进入 `AUTH-S3-BE-01 部门基础服务`。
