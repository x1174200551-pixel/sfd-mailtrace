# AUTH-S1-QA-01 数据权限回归验收记录

| 项目 | 内容 |
|------|------|
| 验收时间 | 2026-07-28 15:35 CST |
| 状态 | ✅ 通过 |
| 范围 | 管理员/处理人数据权限接口回归、前端构建/lint、临时数据清理 |

## 1. QA 数据

| 数据 | 内容 |
|------|------|
| 前缀 | `AUTH-S1-QA-20260728072620` |
| 临时处理人 A | `authqaa072620`，用户 ID `5` |
| 临时处理人 B | `authqab072620`，用户 ID `6` |
| 自己负责工单 | `AUTH-S1-QA-20260728072620-OWN` |
| 他人负责工单 | `AUTH-S1-QA-20260728072620-OTHER` |
| 未分配工单 | `AUTH-S1-QA-20260728072620-POOL` |

说明：QA 完成后已逻辑删除上述临时用户、工单、事件和操作日志；活跃 QA 工单数、活跃 QA 用户数均为 `0`。

## 2. 接口回归结果

| 用例 | 结果 |
|------|------|
| 管理员按 QA 前缀查询工单 | ✅ 看到 `OWN`、`OTHER`、`POOL` 共 3 条 |
| 处理人 A 领取前查询工单 | ✅ 只看到 `OWN`、`POOL` |
| 处理人 A 查询处理人 B 工单详情 | ✅ 返回 403，错误码 `40302` |
| 处理人 A 查询处理人 B 工单附件 | ✅ 返回 403，错误码 `40302` |
| 处理人 A 查询自己负责工单详情 | ✅ 成功 |
| 处理人 A 查询未分配工单详情 | ✅ 成功 |
| 处理人 A 领取未分配工单 | ✅ 成功；负责人变为处理人 A，状态变为 `PROCESSING` |
| 处理人 B 再领取已被领取工单 | ✅ 失败，不能抢占 |
| 处理人 A 领取后查询工单 | ✅ 只看到 `OWN`、`POOL` |
| 处理人 B 领取后查询工单 | ✅ 只看到 `OTHER` |
| 处理人 A 工单统计 | ✅ `totalCount=2`、`processingCount=2` |
| 处理人 B 工单统计 | ✅ `totalCount=1`、`processingCount=1` |
| 处理人 A 工作台统计 | ✅ `totalCount=2`、`activeCount=2` |
| 处理人 B 工作台统计 | ✅ `totalCount=1`、`activeCount=1` |
| 处理人 A 客户只读聚合 | ✅ 只看到 `own`、`pool` 客户邮箱 |
| 处理人 B 客户只读聚合 | ✅ 只看到 `other` 客户邮箱 |

## 3. 页面与构建验证

```bash
curl -s -o /tmp/mailtrace_frontend_authqa.html -w '%{http_code}' http://127.0.0.1:5173/
```

结果：`200`

```bash
cd /Users/tanzhixing/workSpace/sfd-mailtrace/frontend
pnpm build
pnpm lint
```

结果：

| 命令 | 结果 |
|------|------|
| `pnpm build` | ✅ 通过 |
| `pnpm lint` | ✅ 通过；保留既有 `fetchFetchLogStats`、`fetchSendLogStats` hook 依赖 warning |

```bash
cd /Users/tanzhixing/workSpace/sfd-mailtrace/backend
mvn -q test
```

结果：✅ 通过

```bash
cd /Users/tanzhixing/workSpace/sfd-mailtrace
git diff --check
```

结果：✅ 通过

## 4. 注意事项

1. 第一次接口断言发现处理人列表仍返回全量，定位为本地 8080 仍在运行旧后端进程。
2. 重启后端并加载当前工作区代码后，数据权限断言全部通过。
3. AUTH-S1 当前已完成后端收口、领取接口、前端权限体验和接口回归；下一阶段可进入 AUTH-S2 RBAC 配置化。
