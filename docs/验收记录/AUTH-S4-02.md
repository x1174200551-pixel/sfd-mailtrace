# AUTH-S4-02 权限性能检查

| 字段 | 内容 |
|------|------|
| 任务编号 | AUTH-S4-02 |
| 任务名称 | 权限性能检查 |
| 阶段 | AUTH-S4 权限验收与硬化 |
| 编写时间 | 2026-07-29 |

## 1. 索引覆盖情况

### mt_ticket（工单表）
已有 8 个索引，数据范围查询核心索引正常：

| 索引 | 列 | 覆盖场景 | 状态 |
|------|-----|---------|------|
| `idx_mt_ticket_assignee` | `assignee_id` | 数据范围 `assignee_id IN (...)` 过滤 | ✅ |
| `idx_mt_ticket_status` | `status` | 按状态查询 | ✅ |
| `idx_mt_ticket_customer_email` | `customer_email` | 客户查询 | ✅ |
| `idx_mt_ticket_created_at` | `created_at` | 排序/时间范围 | ✅ |

### mt_department（部门表）
| 索引 | 列 | 覆盖场景 | 状态 |
|------|-----|---------|------|
| `idx_mt_department_parent` | `parent_id, sort_order` | 部门树递归查询 | ⚠️ 缺少 `is_enabled` |
| `idx_mt_department_leader` | `leader_user_id` | 负责人查询 | ✅ |

### mt_user_department（用户部门关系表）
| 索引 | 列 | 覆盖场景 | 状态 |
|------|-----|---------|------|
| `uk_mt_user_department` | `user_id, department_id` (UNIQUE) | 关系唯一约束 | ✅ |
| `idx_mt_user_department_user` | `user_id, is_primary` | 用户主部门查询 | ✅ |
| `idx_mt_user_department_department` | `department_id, is_primary` | 部门成员查询 | ✅ |

### 建议新增索引
```sql
-- 覆盖工单分页最常用的查询模式：assignee_id 过滤 + is_deleted 筛选 + created_at 排序
CREATE INDEX idx_mt_ticket_scope_query ON mt_ticket (assignee_id, is_deleted, created_at DESC);
```

---

## 2. 性能风险分析

### 🔴 高优先级

| 风险 | 说明 | 影响 | 建议 |
|------|------|------|------|
| **部门递归 N+1 查询** | `DepartmentMemberService.collectDeptAndChildren()` 使用 Java 递归，每层一个 SQL | 5 层部门树约 121 次查询；无上限保护 | 使用 `dept_path` 字段做 `LIKE` 查询，或迁移至 MySQL 8+ `WITH RECURSIVE` CTE |
| **数据范围反复解析** | 同一个请求内 `resolveDeptScopeMemberIds()` 重复调用多次（工作台统计 6 次、待办 5 次） | 每次请求触发完整部门数遍历多次 | 添加请求级缓存，首次解析后缓存到当前请求结束 |

### 🟡 中优先级

| 风险 | 说明 | 建议 |
|------|------|------|
| **缺少复合索引** | `mt_ticket` 页查询按 `assignee_id` 过滤 + `created_at` 排序，无覆盖索引导致文件排序 | 新增 `idx_mt_ticket_scope_query(assignee_id, is_deleted, created_at DESC)` |
| **`dept_path` 字段闲置** | V13 迁移已预留 `dept_path` 字段但未使用 | 填充 `dept_path` 数据后，下级部门查询一次 SQL 即可完成 |
| **工单详情 N+1** | `toDetailVO()` 每次查询工单消息+事件两条额外 SQL | 可改为批量查询或延迟加载 |

### 🟢 低优先级

| 风险 | 说明 |
|------|------|
| `OR assignee_id IS NULL` 在大 `IN` 列表时影响索引范围扫描效率 | 可考虑拆为两个查询 UNION |
| `resolveUserName` / `resolveMailboxName` 逐行 `selectById` | 数据量小时影响可忽略 |
| `is_deleted` 不在数据范围相关索引中 | 表逻辑删除字段，过滤精度取决于 `@TableLogic` 配置 |

---

## 3. 性能基线（需运行时环境补充）

以下指标需在**有实际数据量的数据库环境**中验证：

| 场景 | 测量指标 | 预期 |
|------|---------|------|
| 工单列表（处理人，5000 条数据） | 响应时间 | < 500ms |
| 工单列表（主管，部门下 100 人） | 响应时间 | < 800ms |
| 工作台统计（主管） | 响应时间 | < 1s |
| 部门树加载（20 个部门） | 响应时间 | < 200ms |
| 部门成员递归解析（5 层深度） | SQL 次数 | 应优化为 1 次 |

---

## 4. 结论

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 索引覆盖完整性 | ✅ | 核心索引已存在，建议新增一个覆盖索引 |
| 部门递归性能 | ⚠️ | Java 递归 N+1 问题，建议用 `dept_path` 或 CTE 优化 |
| 数据范围重复解析 | ⚠️ | 建议加请求级缓存 |
| 无阻塞性性能问题 | ✅ | 当前数据量下可正常运行 |
| 代码层面可优化项 | 3 项 | 详见上表 |

**总体结论：** 当前实现无功能性性能缺陷，但在数据量增长后（> 1 万工单、> 10 个部门层级）部门递归和重复解析会成为瓶颈。建议在数据量增长前完成 `dept_path` 优化和请求级缓存。

---

*验收人：* （待运行环境验证后填写）
*验证日期：* （待运行环境验证后填写）
