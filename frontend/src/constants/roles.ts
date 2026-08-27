import type { RoleFormState } from '../types/role'
import type { RoleCode, UserFormState } from '../types/user'

export const builtInRoleOptions: Array<{ label: string; value: RoleCode }> = [
  { label: '管理员', value: 'ADMIN' },
  { label: '客服处理人', value: 'AGENT' },
]

export const roleProfiles: Record<RoleCode, {
  title: string
  subtitle: string
  menuScope: string
  dataScope: string
  permissionCount: number
  actions: string[]
}> = {
  ADMIN: {
    title: '管理员',
    subtitle: '拥有全部后台配置、工单处理和系统维护权限',
    menuScope: '全部菜单',
    dataScope: '全部范围',
    permissionCount: 83,
    actions: ['用户管理', '邮箱配置', '分配规则', 'SLA 策略', '工单处理', '系统配置'],
  },
  AGENT: {
    title: '客服处理人',
    subtitle: '处理自己负责和未分配池内的工单，查看相关客户数据',
    menuScope: '工作台、全部工单、客户管理',
    dataScope: '自己范围',
    permissionCount: 20,
    actions: ['查看工单', '领取工单', '回复客户', '内部备注', '转派工单', '查看客户'],
  },
}

export function roleLabel(roleCode: string) {
  return roleProfiles[roleCode]?.title || roleCode || '-'
}

export function getRoleProfile(roleCode: string) {
  return roleProfiles[roleCode] || {
    title: roleLabel(roleCode),
    subtitle: '自定义业务角色',
    menuScope: '按角色权限配置',
    dataScope: '按默认数据范围',
    permissionCount: 0,
    actions: ['按权限清单生效'],
  }
}

export const emptyUserForm: UserFormState = {
  account: '',
  displayName: '',
  email: '',
  roleCode: 'AGENT',
  departmentId: null,
  password: '',
  enabled: true,
}

export const emptyRoleForm: RoleFormState = {
  roleName: '',
  roleDesc: '',
  enabled: true,
  permissionCodes: [],
  dataScopes: [],
}
