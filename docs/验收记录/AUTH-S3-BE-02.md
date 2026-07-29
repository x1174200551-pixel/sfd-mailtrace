# AUTH-S3-BE-02 用户部门关系验收记录

## 任务范围

实现用户主部门关系，并同步收敛用户角色口径：

1. 用户新建支持 `departmentId`，为空时兼容分配到默认部门。
2. 用户编辑支持修改主部门。
3. 用户列表和详情型返回补充主部门信息。
4. 用户角色从多角色叠加收敛为单角色。
5. 权限解析只取用户主角色，不再合并历史多角色权限。
6. 前端用户管理移除附加角色勾选，角色改为单选，新增主部门选择和列表展示。

## 代码交付

| 类型 | 文件 |
|------|------|
| 用户 Service | `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/user/UserService.java` |
| 权限解析 | `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/security/PermissionService.java` |
| 用户请求/响应 | `backend/src/main/java/com/ntn/fziot/mailtrace/interfaces/vo/user/` |
| 单测 | `backend/src/test/java/com/ntn/fziot/mailtrace/application/bizservice/user/UserServiceTest.java` |
| 前端用户管理 | `frontend/src/App.tsx` |

## 业务规则

1. 当前版本一个用户同一时间只能绑定一个角色。
2. `roleCodes` 字段保留接口兼容，但只能为空、只包含主角色，不能携带其他角色。
3. 后端保存用户角色时只写一条 `mt_user_role`，并标记 `is_primary=1`。
4. 权限查询按 `mt_user_role.is_primary` 优先，只取一个角色；没有用户角色关系时才回退 `mt_user.role_code`。
5. 用户主部门写入 `mt_user_department`，并标记 `is_primary=1`。
6. 新建/编辑传入部门必须存在且启用；为空时使用 `DEFAULT` 默认部门。

## 接口冒烟

临时后端端口：`18186`

| 验证项 | 结果 |
|--------|------|
| `POST /api/v1/auth/login`，账号 `admin/admin123` | `code=0` |
| `GET /api/v1/departments?enabled=true` | 返回默认部门 |
| `GET /api/v1/users?page=1&size=5` | 用户记录返回 `departmentName=默认部门` |
| `POST /api/v1/users` 携带 `roleCodes=["AGENT","ADMIN"]` | HTTP 400，`code=40001`，提示当前版本一个用户只能分配一个角色 |

## 追加修复：前端菜单权限兜底

谭总反馈：只勾选邮箱管理权限时，前端仍展示工作空间和工单中心，点击后才提示无权限。

修复内容：

1. 前端当前菜单不在可见权限菜单内时，不再固定回退到「工作台」。
2. 改为回退到第一个真实可见菜单。
3. 临时账号仅具备 `menu:mail_management` 和 `menu:mailboxes` 时，侧边栏只展示「邮箱配置」。

验证结果：

| 验证项 | 结果 |
|--------|------|
| 临时角色权限 | 仅返回 `menu:mail_management`、`menu:mailboxes` |
| 临时账号登录页面 | 侧边栏只展示「邮箱配置」 |
| 工作空间/工单中心 | 未展示 |
| 临时用户/角色数据 | 已清理，剩余数量为 0 |
| 临时拉信日志副作用 | 已清理 `mt_mail_fetch_log` ids `633`、`634` |

## 追加修复：邮箱配置页面权限匹配

谭总反馈：角色只勾选「邮箱配置」后，进入邮箱配置页面仍显示无权限。

修复内容：

1. 前端邮箱配置页面不再使用管理员身份判断，改为 `menu:mailboxes` 或 `mailbox:read` 可读。
2. 前端侧边栏不再要求父级菜单权限必须存在；只要存在可见子菜单，就展示对应分组。
3. 新增、编辑、启停、删除、测试连接仍分别受 `mailbox:create`、`mailbox:update`、`mailbox:enable`、`mailbox:delete`、`mailbox:test_connection` 控制。
4. 后端邮箱列表接口同步接受 `menu:mailboxes` 作为只读入口，避免前端可进入但 API 返回无权限。

验证结果：

| 验证项 | 结果 |
|--------|------|
| `MailboxServiceTest` 仅具备 `menu:mailboxes` | 可查询邮箱列表 |
| `MailboxServiceTest` 无 `menu:mailboxes` 且无 `mailbox:read` | 返回 `40302` |
| 前端邮箱配置页面 | 可读权限和操作按钮权限已拆分 |

## 验证记录

| 验证项 | 结果 |
|--------|------|
| `cd backend && mvn -q -Dtest=MailboxServiceTest test` | 通过 |
| `cd backend && mvn -q -Dtest=UserServiceTest,PermissionServiceTest test` | 通过 |
| `cd backend && mvn -q test` | 通过 |
| `cd frontend && pnpm build` | 通过 |
| `cd frontend && pnpm lint` | 通过，有既有 hooks 警告 |
| `git diff --check` | 通过 |

## 边界说明

本任务只完成用户与主部门的保存、查询和前端接入，不包含：

1. `SUPERVISOR` 主管角色默认权限，进入 `AUTH-S3-BE-03`。
2. `DEPT` / `DEPT_AND_CHILDREN` 数据范围计算，进入 `AUTH-S3-BE-04`。
3. 组织管理前端页面，进入 `AUTH-S3-FE-01/02`，开发前需先补原型并确认。

## 结论

`AUTH-S3-BE-02` 技术验证通过，可进入 `AUTH-S3-BE-03 主管角色启用`。
