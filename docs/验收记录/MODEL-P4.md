# MODEL-P4 建单策略链路验收记录

| 项目 | 结论 |
|------|------|
| 任务编号 | MODEL-P4 |
| 当前状态 | ✅ 已完成 |
| 开始时间 | 2026-08-26 16:46 CST |
| 完成时间 | 2026-08-26 17:05 CST |
| 前置门禁 | MODEL-P3 ✅ |
| 下一阶段 | MODEL-P5-PG 新模型前端原型补齐与确认 |

## 完成范围

| 范围 | 状态 | 结论 |
|------|------|------|
| 建单归属 | ✅ | 校验启用企业/邮箱，写入企业、邮箱、客户和策略快照 |
| 客户档案 | ✅ | 邮箱规范化；按 `enterprise_id + email` 原子新增或更新 |
| 自动回复 | ✅ | 只使用邮箱绑定的同企业启用模板；缺失时不发送并记录事件 |
| SLA | ✅ | 只使用邮箱绑定的同企业启用策略和日历；未绑定不计算 |
| 自动分配 | ✅ | 只加载邮箱绑定规则组，忽略历史 `DEFAULT` 规则，校验处理人邮箱权限 |
| fallback | ✅ | 仅 `DEFAULT_ASSIGNEE` 使用默认处理人，`NONE` 保持待分配 |
| 发件日志 | ✅ | 自动回复、分配通知、SLA 提醒补齐企业、工单和模板元数据 |
| 工单响应 | ✅ | 列表、工作台摘要、详情返回企业和策略快照字段 |

## 验收证据

1. `mvn -q -Dtest=TicketBizServiceTest,AutoReplyServiceTest,AssignmentRuleServiceTest,SlaDeadlineServiceTest,SlaCheckServiceTest test`：P4 核心单元测试通过。
2. `cd backend && mvn -q test`：后端全量测试通过。
3. `NACOS_CONFIG_ENABLED=false mvn -q -Dtest=P4TicketStrategyLocalIT test`：本地真实数据库完成两企业、配置邮箱、空策略邮箱的建单矩阵；SMTP 使用 mock，测试业务数据随事务回滚。
4. 本地 Flyway schema version 19，本地运行数据库为 MySQL 8.0。MySQL 5.7 兼容结论来自 SQL 静态核对，客户 upsert 使用 5.7 支持语法，未虚报为 MySQL 5.7 实机验证。
5. 集成测试使用 `webEnvironment=MOCK`，没有监听 8080；验收期间未恢复用户已停止的后台服务。

## 关键验收结论

1. 企业 A 邮箱绑定模板、SLA、规则组后，建单写入企业、邮箱、客户、模板、SLA、规则组、命中规则和处理人快照。
2. 空策略邮箱不发送自动回复、不写 SLA 截止时间、不自动分配；即使存在默认处理人，fallback 为 `NONE` 时仍保持待分配。
3. 同企业相同邮箱复用同一客户档案；不同企业相同邮箱生成不同客户档案。
4. 跨企业配置、无目标邮箱操作权限的规则处理人和默认处理人均不能生效。
5. 历史全局默认模板、默认 SLA、`DEFAULT` 分配规则不再影响新建工单。

## 主要实现位置

- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/ticket/TicketBizService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/ticket/AutoReplyService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/sla/SlaDeadlineService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/assignment/AssignmentRuleService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/application/bizservice/mailsend/MailSendService.java`
- `backend/src/main/java/com/ntn/fziot/mailtrace/repox/mysql/mapper/CustomerMapper.java`
- `backend/src/test/java/com/ntn/fziot/mailtrace/integration/P4TicketStrategyLocalIT.java`

## 阶段边界和下一门禁

P4 没有修改前端。现有邮箱、用户、分配规则、SLA 等原型均基于旧模型，且没有企业管理页和用户企业/邮箱授权交互。MODEL-P5 开发前先补齐并确认新模型原型，确认后再进行前端接口接入和浏览器联调。

## 回滚边界

应用可整体回滚 P4 建单链路代码；V18/V19 字段和历史策略快照继续保留，不清理业务数据，不做破坏性数据库降级。
