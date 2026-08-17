export function dataResourceLabel(resourceType: string) {
  return ({ TICKET: '工单数据', CUSTOMER: '客户数据', DASHBOARD: '工作台数据' } as Record<string, string>)[resourceType] || resourceType
}

export function dataScopeLabel(scopeCode: string) {
  return ({
    ALL: '全部范围',
    SELF: '自己范围',
    DEPT: '本部门',
    DEPT_AND_CHILDREN: '本部门及下级',
  } as Record<string, string>)[scopeCode] || scopeCode
}

export function dataScopeDesc(resourceType: string, scopeCode: string) {
  const scope = dataScopeLabel(scopeCode)
  if (resourceType === 'TICKET') {
    return ({
      ALL: '可查看全部工单',
      SELF: '自己负责工单 + 未分配池',
      DEPT: '本部门成员负责工单 + 未分配池',
      DEPT_AND_CHILDREN: '本部门及下级部门成员负责工单 + 未分配池',
    } as Record<string, string>)[scopeCode] || scope
  }
  if (resourceType === 'CUSTOMER') {
    return ({
      ALL: '全部客户聚合数据',
      SELF: '自己可见工单关联客户',
      DEPT: '本部门可见工单关联客户',
      DEPT_AND_CHILDREN: '本部门及下级部门可见工单关联客户',
    } as Record<string, string>)[scopeCode] || scope
  }
  if (resourceType === 'DASHBOARD') {
    return ({
      ALL: '全部工作台统计数据',
      SELF: '自己负责工单 + 未分配池统计',
      DEPT: '本部门成员负责工单 + 未分配池统计',
      DEPT_AND_CHILDREN: '本部门及下级部门成员负责工单 + 未分配池统计',
    } as Record<string, string>)[scopeCode] || scope
  }
  return scope
}
