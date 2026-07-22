# P1-W0-C01-system 系统参数/编号规则接口契约

## 范围

- 本阶段只实现业务可见的工单编号规则配置。
- 通用系统参数不提供业务页面维护入口，不暴露参数键、参数表、新增参数等能力。
- 前端准入：`PG-04` 已由谭总确认后，才允许开发编号规则页面。

## GET /api/v1/sys-params/ticket-number-rule

查询当前工单编号规则，并返回下一工单号预览。

### 响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "enabled": true,
    "prefix": "TCK",
    "dateFormat": "yyyyMMdd",
    "seqLength": 4,
    "separator": "-",
    "description": "客户来信自动建单时生成唯一工单号；邮件线程关联会优先匹配主题中的工单号。",
    "todayDate": "2026-07-22",
    "dateKey": "20260722",
    "usedSeq": 42,
    "nextSeq": "0043",
    "nextTicketNo": "TCK-20260722-0043",
    "subjectPreview": "Re: TCK-20260722-0043",
    "updatedAt": "2026-07-22T19:44:00"
  }
}
```

## POST /api/v1/sys-params/ticket-number-rule/preview

按页面输入生成预览，不保存。

### 请求

```json
{
  "enabled": true,
  "prefix": "TCK",
  "dateFormat": "yyyyMMdd",
  "seqLength": 4,
  "separator": "-",
  "description": "客户来信自动建单时生成唯一工单号；邮件线程关联会优先匹配主题中的工单号。"
}
```

### 校验

- `prefix`：2-8 位大写英文或数字。
- `dateFormat`：仅支持 `yyyyMMdd`、`yyyyMM`、`yyyy`。
- `seqLength`：3-8。
- `separator`：仅支持 `-`、`_` 或空字符串。

## PUT /api/v1/sys-params/ticket-number-rule

保存编号规则。保存后仅影响后续新建工单，历史工单号不回写。

### 请求

同预览接口。

### 响应

同查询接口，返回保存后的最新预览。

## 实现对应

- 后端：`SysParamController`、`TicketNumberRuleService`、`SysParamEntity`、`TicketSeqEntity`。
- 前端：`App.tsx` 编号规则内容区。
- 数据：`mt_sys_param` 存储业务配置，`mt_ticket_seq` 提供当前日期维度流水预览。
