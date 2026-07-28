# AUTH-S2.5-BE-03 用户多角色分配验收记录

| 字段 | 内容 |
|------|------|
| 验收时间 | 2026-07-28 17:22 CST |
| 当前状态 | ✅ 已完成 |
| 关联原型 | `docs/原型设计/16、邮件工单系统-角色配置管理原型.html` |
| 主要代码 | `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/user/UserService.java`；`backend/src/main/java/com/ntn/fziot/mailtrace/interfaces/vo/user/UserCreateRequest.java`；`backend/src/main/java/com/ntn/fziot/mailtrace/interfaces/vo/user/UserUpdateRequest.java`；`backend/src/main/java/com/ntn/fziot/mailtrace/interfaces/vo/user/UserVO.java` |

## 1. 实现范围

1. 用户新建和编辑请求新增 `roleCodes`，支持给用户保存一个或多个角色。
2. 保留 `roleCode` 作为主角色展示口径，并继续写入 `mt_user.role_code` 兼容旧逻辑。
3. 保存用户时同步替换 `mt_user_role`，主角色行标记 `is_primary = 1`，其他角色标记为非主角色。
4. 用户列表响应新增 `roleCodes`，前端后续可展示完整角色集合。
5. 登录后的权限和数据范围继续由 `PermissionService` 从 `mt_user_role` 合并，无需新增登录接口字段。
6. 当前阶段不引入指定邮箱和客户标签数据范围。

## 2. Service 层步骤

1. `createUser`：校验 `user:create`，校验主角色和角色集合，校验账号唯一，写入用户主表，同步完整用户角色关系，记录日志，返回详情。
2. `updateUser`：校验 `user:update`，查询目标用户，校验主角色和角色集合，执行自我保护，更新用户主表，同步完整用户角色关系，记录日志，返回详情。
3. `syncUserRoles`：物理清理目标用户旧角色关系，按请求角色集合重建 `mt_user_role`，主角色置为 `is_primary = 1`。
4. `roleCodes`：查询用户角色关系并按主角色优先返回完整角色编码清单；没有关系数据时回退 `mt_user.role_code`。

## 3. 验证记录

| 检查项 | 结果 |
|--------|------|
| 定向单测 | `mvn -q -Dtest=UserServiceTest test` 通过 |
| 后端全量测试 | `mvn -q test` 通过 |
| 临时服务启动 | 18182 临时端口启动成功，Flyway 当前 version 12 |
| 接口冒烟 | 临时自定义角色 + 临时用户创建成功，用户响应 `roleCodes=["AGENT","QA_TEMP_*"]` |
| 登录冒烟 | 临时用户登录后 `roles=["AGENT","QA_TEMP_*"]` |
| 数据清理 | 临时用户和临时角色 SQL 清理后计数均为 0 |

## 4. 边界说明

1. `roleCode` 仍是主角色，不等于完整权限集合；完整集合以后端返回的 `roleCodes` 和登录态 `roles` 为准。
2. 本阶段只做后端保存链路，角色管理前端页面由 `AUTH-S2.5-FE-01` 实现。
3. 自定义角色的数据范围仍遵循 `AUTH-S2.5-BE-01/02` 的白名单，不支持 `MAILBOX`、`CUSTOMER_TAG`。
