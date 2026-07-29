# AUTH-S3-BE-01 部门基础服务验收记录

## 任务范围

实现组织管理的后端基础能力：

1. 部门树查询。
2. 部门详情查询。
3. 新建部门。
4. 编辑部门名称、说明、负责人、启停状态和排序。
5. 启用或停用部门。
6. 初始化组织管理菜单和部门操作权限。

## 代码交付

| 类型 | 文件 |
|------|------|
| 权限迁移 | `backend/src/main/resources/db/migration/V14__department_management_permissions.sql` |
| Service | `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/department/DepartmentService.java` |
| Controller | `backend/src/main/java/com/ntn/fziot/mailtrace/interfaces/api/department/DepartmentController.java` |
| VO/Request | `backend/src/main/java/com/ntn/fziot/mailtrace/interfaces/vo/department/` |
| 单测 | `backend/src/test/java/com/ntn/fziot/mailtrace/application/bizservice/department/DepartmentServiceTest.java` |

## 接口范围

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| `GET` | `/v1/departments` | 查询部门树 | `department:read` |
| `GET` | `/v1/departments/{id}` | 查询部门详情 | `department:read` |
| `POST` | `/v1/departments` | 新建部门 | `department:create` |
| `PUT` | `/v1/departments/{id}` | 编辑部门 | `department:update` |
| `PATCH` | `/v1/departments/{id}/enabled` | 启用或停用部门 | `department:enable` |

## 业务规则

1. 部门编码创建时统一转大写并校验唯一。
2. 新建子部门时按父部门路径生成 `dept_path`。
3. 负责人必须是存在且启用的系统用户。
4. 默认部门 `DEFAULT` 不允许停用。
5. 存在启用下级部门时，不允许停用当前部门。
6. 部门写操作写入 `mt_operation_log`，模块为 `DEPARTMENT`。

## 验证记录

| 验证项 | 结果 |
|--------|------|
| `cd backend && mvn -q -Dtest=DepartmentServiceTest test` | 通过 |
| `cd backend && mvn -q test` | 通过 |
| `git diff --check` | 通过 |
| 临时端口 `18185` 启动后端 | 通过 |
| Flyway 启动验证 | 已验证 14 个迁移，当前 schema version = 14 |
| `curl -s http://127.0.0.1:18185/api/v1/system/health` | 返回 `code=0`，`status=UP` |

## 边界说明

本任务不包含：

1. 用户主部门保存和查询扩展，进入 `AUTH-S3-BE-02`。
2. `SUPERVISOR` 角色默认权限，进入 `AUTH-S3-BE-03`。
3. `DEPT` / `DEPT_AND_CHILDREN` 数据范围计算，进入 `AUTH-S3-BE-04`。
4. 组织管理前端页面，进入 `AUTH-S3-FE-01/02`，且开发前需先补原型并确认。

## 结论

`AUTH-S3-BE-01` 技术验证通过，可进入 `AUTH-S3-BE-02 用户部门关系`。
