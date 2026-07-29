# AUTH-R1-05 权限审后加固验收记录

| 字段 | 内容 |
|------|------|
| 验收时间 | 2026-07-29 |
| 验收范围 | 权限体系与数据权限阶段审后加固 |
| 结论 | 通过 |

## 1. 本轮加固内容

1. 客户只读数据范围：当非全部范围用户解析出的可见负责人为空时，Service 层直接返回空列表或 404，Mapper 层空范围分支改为 `1 = 0`，避免 SQL 兜底放宽。
2. 角色数据范围：角色配置支持 `SELF`、`DEPT`、`DEPT_AND_CHILDREN`、`ALL`，前端数据范围选择与后端支持能力保持一致。
3. 接口权限：新增 `@RequirePermission` 注解和 MVC 拦截器，Controller 层声明入口权限，Service 层保留权限校验作为最终防线。
4. 前端权限：分配规则、SLA 策略、工作日历、编号规则、通知模板、收件记录、发件记录等页面读取入口从 `isAdmin` 改为具体权限码判断。
5. 通用上传：`/api/v1/files/upload` 纳入 `ticket_attachment:upload` 权限控制，避免真实上传入口漏校验。

## 2. 验证结果

| 命令 | 结果 |
|------|------|
| `mvn -q -Dtest=CustomerReadonlyServiceTest,RoleManagementServiceTest,RequirePermissionInterceptorTest test` | 通过 |
| `mvn -q test` | 通过 |
| `pnpm lint` | 通过，无告警 |
| `pnpm build` | 通过，仅保留 Vite chunk 体积提示 |

## 3. 剩余建议

1. 当前注解式接口权限已经具备骨架，后续新增 Controller 必须优先声明 `@RequirePermission`。
2. Service 层权限校验暂时保留，避免定时任务、内部调用或遗漏注解时失去最终防线。
3. 后续如继续细化按钮级体验，可把编辑、删除、启停等动作按钮全面替换为对应权限码控制。
