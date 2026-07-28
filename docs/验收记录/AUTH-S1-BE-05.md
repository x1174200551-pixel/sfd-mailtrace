# AUTH-S1-BE-05 未分配领取接口验收记录

| 项目 | 内容 |
|------|------|
| 验收时间 | 2026-07-28 15:15 CST |
| 状态 | ✅ 技术验收通过 |
| 范围 | 未分配工单领取接口、服务逻辑、事件与操作日志、单测 |

## 1. 交付内容

1. 新增接口：`POST /api/v1/tickets/{id}/claim`。
2. 新增服务方法：`TicketBizService.claimTicket`。
3. 领取规则：
   - 管理员和处理人均可访问。
   - 当前用户必须能看见该工单。
   - 仅允许领取未分配工单。
   - 已关闭/已取消工单不能领取。
   - 使用 `WHERE assignee_id IS NULL` 条件更新，避免并发抢占。
   - 待分配工单领取后自动进入处理中。
4. 领取后写入生命周期事件：
   - 事件类型：`ASSIGNED`
   - 文案：`领取未分配工单：<处理人名称>`
5. 领取后写入操作日志：
   - 操作类型：`CLAIM`

## 2. 验证命令

```bash
cd /Users/tanzhixing/workSpace/sfd-mailtrace/backend
mvn -q -Dtest=TicketBizServiceTest test
mvn -q test
```

```bash
cd /Users/tanzhixing/workSpace/sfd-mailtrace
git diff --check
```

## 3. 验证结果

| 用例 | 结果 |
|------|------|
| 未分配工单可领取 | ✅ 通过 |
| 已分配工单不可领取 | ✅ 通过 |
| 并发领取条件更新失败时返回业务错误 | ✅ 通过 |
| `TicketBizServiceTest` | ✅ 通过 |
| 后端全量 `mvn -q test` | ✅ 通过 |
| `git diff --check` | ✅ 通过 |

## 4. 下一步

进入 `AUTH-S1-FE-01` 前端权限体验调整。由于下一步涉及前端页面按钮、提示和交互，需要先按谭总要求确认原型/交互范围。
