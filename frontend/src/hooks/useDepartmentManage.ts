import { useCallback, useEffect, useMemo, useState } from 'react'
import { message } from 'antd'
import { departmentApi } from '../api/departments'
import type {
  DepartmentFormMode,
  DepartmentFormState,
  DepartmentMemberPageResponse,
  DepartmentNode,
  DepartmentStats,
} from '../types/department'
import type { ManagedUser } from '../types/user'
import { findDeptNode, flattenDepartments } from '../utils/departments'

const emptyDeptForm: DepartmentFormState = {
  parentId: 0,
  deptName: '',
  deptDesc: '',
  leaderUserId: 0,
  enabled: true,
}

type UseDepartmentManageParams = {
  canReadDepartments: boolean
  onAuthExpired: (error: unknown) => boolean
}

export function useDepartmentManage({ canReadDepartments, onAuthExpired }: UseDepartmentManageParams) {
  const [orgTree, setOrgTree] = useState<DepartmentNode[]>([])
  const [orgStats, setOrgStats] = useState<DepartmentStats | null>(null)
  const [orgStatsLoading, setOrgStatsLoading] = useState(false)
  const [orgTreeLoading, setOrgTreeLoading] = useState(false)
  const [orgError, setOrgError] = useState('')
  const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null)
  const [deptFormOpen, setDeptFormOpen] = useState(false)
  const [deptFormMode, setDeptFormMode] = useState<DepartmentFormMode>('create')
  const [deptFormSubmitting, setDeptFormSubmitting] = useState(false)
  const [deptFormError, setDeptFormError] = useState('')
  const [deptMoveOpen, setDeptMoveOpen] = useState(false)
  const [deptMoveParentId, setDeptMoveParentId] = useState<number>(0)
  const [deptMoveSubmitting, setDeptMoveSubmitting] = useState(false)
  const [deptMemberKeyword, setDeptMemberKeyword] = useState('')
  const [deptMemberRoleFilter, setDeptMemberRoleFilter] = useState('ALL')
  const [deptMemberPage, setDeptMemberPage] = useState(1)
  const [deptMemberPageSize, setDeptMemberPageSize] = useState(10)
  const [deptMembersData, setDeptMembersData] = useState<DepartmentMemberPageResponse | null>(null)
  const [deptMembersLoading, setDeptMembersLoading] = useState(false)
  const [deptMembersError, setDeptMembersError] = useState('')
  const [addDeptMemberOpen, setAddDeptMemberOpen] = useState(false)
  const [deptCandidateKeyword, setDeptCandidateKeyword] = useState('')
  const [deptCandidatesData, setDeptCandidatesData] = useState<DepartmentMemberPageResponse | null>(null)
  const [deptCandidatesLoading, setDeptCandidatesLoading] = useState(false)
  const [selectedCandidateIds, setSelectedCandidateIds] = useState<number[]>([])
  const [deptMemberSubmitting, setDeptMemberSubmitting] = useState(false)
  const [deptLeaderOpen, setDeptLeaderOpen] = useState(false)
  const [deptLeaderUserId, setDeptLeaderUserId] = useState<number>(0)
  const [deptLeaderSubmitting, setDeptLeaderSubmitting] = useState(false)
  const [deptConfirmOpen, setDeptConfirmOpen] = useState(false)
  const [deptConfirmTarget, setDeptConfirmTarget] = useState<DepartmentNode | null>(null)
  const [deptEnableSubmitting, setDeptEnableSubmitting] = useState(false)
  const [deptForm, setDeptForm] = useState<DepartmentFormState>(emptyDeptForm)

  const orgDeptOptions = useMemo(() => flattenDepartments(orgTree), [orgTree])
  const hasRootDepartment = orgTree.length > 0
  const selectedDeptNode = useMemo(() => selectedDeptId ? findDeptNode(orgTree, selectedDeptId) : null, [orgTree, selectedDeptId])
  const movableParentOptions = useMemo(
    () => orgDeptOptions.filter((department) => department.id !== selectedDeptId && !(selectedDeptNode && findDeptNode(selectedDeptNode.children || [], department.id))),
    [orgDeptOptions, selectedDeptId, selectedDeptNode],
  )

  const fetchOrgTree = useCallback(async () => {
    if (!canReadDepartments) {
      setOrgTree([])
      setOrgError('当前账号没有组织管理权限')
      return
    }
    setOrgTreeLoading(true)
    setOrgError('')
    try {
      const data = await departmentApi.list()
      setOrgTree(data)
      const flat = flattenDepartments(data)
      setSelectedDeptId((current) => (
        current && flat.some((department) => department.id === current)
          ? current
          : flat[0]?.id ?? null
      ))
    } catch (error) {
      if (onAuthExpired(error)) return
      setOrgError(error instanceof Error ? error.message : '组织架构加载失败')
    } finally {
      setOrgTreeLoading(false)
    }
  }, [canReadDepartments, onAuthExpired])

  const fetchOrgStats = useCallback(async () => {
    if (!canReadDepartments) return
    setOrgStatsLoading(true)
    try {
      setOrgStats(await departmentApi.stats())
    } catch (error) {
      if (onAuthExpired(error)) return
      setOrgError(error instanceof Error ? error.message : '组织统计加载失败')
    } finally {
      setOrgStatsLoading(false)
    }
  }, [canReadDepartments, onAuthExpired])

  const fetchDeptMembers = useCallback(async () => {
    if (!selectedDeptId || !canReadDepartments) return
    setDeptMembersLoading(true)
    setDeptMembersError('')
    try {
      setDeptMembersData(await departmentApi.members(selectedDeptId, {
        keyword: deptMemberKeyword.trim(),
        roleCode: deptMemberRoleFilter === 'ALL' ? undefined : deptMemberRoleFilter,
        page: deptMemberPage,
        size: deptMemberPageSize,
      }))
    } catch (error) {
      if (onAuthExpired(error)) return
      setDeptMembersError(error instanceof Error ? error.message : '部门成员加载失败')
    } finally {
      setDeptMembersLoading(false)
    }
  }, [canReadDepartments, deptMemberKeyword, deptMemberPage, deptMemberPageSize, deptMemberRoleFilter, onAuthExpired, selectedDeptId])

  const fetchDeptCandidates = useCallback(async () => {
    if (!selectedDeptId || !addDeptMemberOpen || !canReadDepartments) return
    setDeptCandidatesLoading(true)
    try {
      setDeptCandidatesData(await departmentApi.memberCandidates(selectedDeptId, {
        keyword: deptCandidateKeyword.trim(),
        page: 1,
        size: 20,
      }))
    } catch (error) {
      if (onAuthExpired(error)) return
      message.error(error instanceof Error ? error.message : '候选成员加载失败')
    } finally {
      setDeptCandidatesLoading(false)
    }
  }, [addDeptMemberOpen, canReadDepartments, deptCandidateKeyword, onAuthExpired, selectedDeptId])

  useEffect(() => {
    void fetchOrgTree()
    void fetchOrgStats()
  }, [fetchOrgStats, fetchOrgTree])

  useEffect(() => {
    void fetchDeptMembers()
  }, [fetchDeptMembers])

  useEffect(() => {
    void fetchDeptCandidates()
  }, [fetchDeptCandidates])

  const refresh = useCallback(() => {
    void fetchOrgTree()
    void fetchOrgStats()
    void fetchDeptMembers()
  }, [fetchDeptMembers, fetchOrgStats, fetchOrgTree])

  const selectDepartment = useCallback((departmentId: number) => {
    setSelectedDeptId(departmentId)
    setDeptMemberPage(1)
  }, [])

  const openCreateDepartment = useCallback((parentId: number) => {
    setDeptFormMode('create')
    setDeptForm({ ...emptyDeptForm, parentId })
    setDeptFormError('')
    setDeptFormOpen(true)
  }, [])

  const openEditDepartment = useCallback((department: DepartmentNode) => {
    setDeptFormMode('edit')
    setDeptForm({
      parentId: department.parentId ?? 0,
      deptName: department.deptName,
      deptDesc: department.deptDesc ?? '',
      leaderUserId: department.leaderUserId ?? 0,
      enabled: department.enabled,
    })
    setDeptFormError('')
    setDeptFormOpen(true)
  }, [])

  const submitDeptForm = useCallback(async () => {
    if (deptFormSubmitting) return
    const name = deptForm.deptName.trim()
    if (!name) {
      setDeptFormError('请输入部门名称')
      return
    }
    setDeptFormError('')
    setDeptFormSubmitting(true)
    try {
      if (deptFormMode === 'create') {
        await departmentApi.create({
          parentId: deptForm.parentId || null,
          deptName: name,
          deptDesc: deptForm.deptDesc.trim(),
          leaderUserId: deptForm.leaderUserId || null,
          enabled: deptForm.enabled,
        })
      } else if (selectedDeptId) {
        await departmentApi.update(selectedDeptId, {
          deptName: name,
          deptDesc: deptForm.deptDesc.trim(),
          leaderUserId: deptForm.leaderUserId || null,
          enabled: deptForm.enabled,
        })
      }
      setDeptFormOpen(false)
      message.success(deptFormMode === 'create' ? '部门已创建' : '部门信息已保存')
      refresh()
    } catch (error) {
      if (onAuthExpired(error)) return
      setDeptFormError(error instanceof Error ? error.message : '保存失败')
    } finally {
      setDeptFormSubmitting(false)
    }
  }, [deptForm, deptFormMode, deptFormSubmitting, onAuthExpired, refresh, selectedDeptId])

  const openMoveDepartment = useCallback((department: DepartmentNode) => {
    setDeptMoveParentId(department.parentId ?? 0)
    setDeptMoveOpen(true)
  }, [])

  const submitDeptMove = useCallback(async () => {
    if (!selectedDeptId || deptMoveSubmitting) return
    setDeptMoveSubmitting(true)
    try {
      await departmentApi.move(selectedDeptId, deptMoveParentId || null)
      setDeptMoveOpen(false)
      message.success('部门已移动')
      void fetchOrgTree()
    } catch (error) {
      if (onAuthExpired(error)) return
      message.error(error instanceof Error ? error.message : '移动失败')
    } finally {
      setDeptMoveSubmitting(false)
    }
  }, [deptMoveParentId, deptMoveSubmitting, fetchOrgTree, onAuthExpired, selectedDeptId])

  const openAddMember = useCallback(() => {
    setDeptCandidateKeyword('')
    setSelectedCandidateIds([])
    setAddDeptMemberOpen(true)
  }, [])

  const submitAddDeptMembers = useCallback(async () => {
    if (!selectedDeptId || deptMemberSubmitting || selectedCandidateIds.length === 0) return
    setDeptMemberSubmitting(true)
    try {
      await departmentApi.addMembers(selectedDeptId, selectedCandidateIds)
      setAddDeptMemberOpen(false)
      message.success(`已添加 ${selectedCandidateIds.length} 名成员`)
      setSelectedCandidateIds([])
      refresh()
      void fetchDeptCandidates()
    } catch (error) {
      if (onAuthExpired(error)) return
      message.error(error instanceof Error ? error.message : '添加成员失败')
    } finally {
      setDeptMemberSubmitting(false)
    }
  }, [deptMemberSubmitting, fetchDeptCandidates, onAuthExpired, refresh, selectedCandidateIds, selectedDeptId])

  const removeDeptMember = useCallback(async (member: ManagedUser) => {
    if (!selectedDeptId || deptMemberSubmitting) return
    setDeptMemberSubmitting(true)
    try {
      await departmentApi.removeMember(selectedDeptId, member.id)
      message.success(`已将 ${member.displayName} 移出当前部门`)
      refresh()
    } catch (error) {
      if (onAuthExpired(error)) return
      message.error(error instanceof Error ? error.message : '移出成员失败')
    } finally {
      setDeptMemberSubmitting(false)
    }
  }, [deptMemberSubmitting, onAuthExpired, refresh, selectedDeptId])

  const openLeaderModal = useCallback(() => {
    setDeptLeaderUserId(selectedDeptNode?.leaderUserId ?? deptMembersData?.records[0]?.id ?? 0)
    setDeptLeaderOpen(true)
  }, [deptMembersData?.records, selectedDeptNode?.leaderUserId])

  const updateDeptLeader = useCallback(async (leaderUserId: number) => {
    if (!selectedDeptId || !leaderUserId || deptLeaderSubmitting) return
    setDeptLeaderSubmitting(true)
    try {
      await departmentApi.updateLeader(selectedDeptId, leaderUserId)
      setDeptLeaderOpen(false)
      const leaderName = deptMembersData?.records.find((member) => member.id === leaderUserId)?.displayName
      message.success(leaderName ? `已将 ${leaderName} 设为负责人` : '负责人已更新')
      void fetchOrgTree()
      void fetchOrgStats()
    } catch (error) {
      if (onAuthExpired(error)) return
      message.error(error instanceof Error ? error.message : '设置负责人失败')
    } finally {
      setDeptLeaderSubmitting(false)
    }
  }, [deptLeaderSubmitting, deptMembersData?.records, fetchOrgStats, fetchOrgTree, onAuthExpired, selectedDeptId])

  const submitDeptLeader = useCallback(async () => {
    await updateDeptLeader(deptLeaderUserId)
  }, [deptLeaderUserId, updateDeptLeader])

  const openEnableConfirm = useCallback((department: DepartmentNode) => {
    setDeptConfirmTarget(department)
    setDeptConfirmOpen(true)
  }, [])

  const submitDeptEnable = useCallback(async () => {
    const target = deptConfirmTarget
    if (!target || deptEnableSubmitting) return
    setDeptEnableSubmitting(true)
    try {
      await departmentApi.setEnabled(target.id, !target.enabled)
      setDeptConfirmOpen(false)
      setDeptConfirmTarget(null)
      message.success(target.enabled ? '部门已停用' : '部门已启用')
      void fetchOrgTree()
      void fetchOrgStats()
    } catch (error) {
      if (onAuthExpired(error)) return
      message.error(error instanceof Error ? error.message : '操作失败')
    } finally {
      setDeptEnableSubmitting(false)
    }
  }, [deptConfirmTarget, deptEnableSubmitting, fetchOrgStats, fetchOrgTree, onAuthExpired])

  return {
    state: {
      orgTree,
      orgStats,
      orgStatsLoading,
      orgTreeLoading,
      orgError,
      selectedDeptId,
      selectedDeptNode,
      orgDeptOptions,
      hasRootDepartment,
      movableParentOptions,
      deptFormOpen,
      deptFormMode,
      deptFormSubmitting,
      deptFormError,
      deptForm,
      deptMoveOpen,
      deptMoveParentId,
      deptMoveSubmitting,
      deptMemberKeyword,
      deptMemberRoleFilter,
      deptMemberPage,
      deptMemberPageSize,
      deptMembersData,
      deptMembersLoading,
      deptMembersError,
      addDeptMemberOpen,
      deptCandidateKeyword,
      deptCandidatesData,
      deptCandidatesLoading,
      selectedCandidateIds,
      deptMemberSubmitting,
      deptLeaderOpen,
      deptLeaderUserId,
      deptLeaderSubmitting,
      deptConfirmOpen,
      deptConfirmTarget,
      deptEnableSubmitting,
    },
    actions: {
      refresh,
      fetchOrgTree,
      fetchDeptMembers,
      fetchDeptCandidates,
      selectDepartment,
      openCreateDepartment,
      openEditDepartment,
      openMoveDepartment,
      openAddMember,
      openLeaderModal,
      openEnableConfirm,
      submitDeptForm,
      submitDeptMove,
      submitAddDeptMembers,
      removeDeptMember,
      updateDeptLeader,
      submitDeptLeader,
      submitDeptEnable,
      setDeptFormOpen,
      setDeptForm,
      setDeptMoveOpen,
      setDeptMoveParentId,
      setDeptMemberKeyword,
      setDeptMemberRoleFilter,
      setDeptMemberPage,
      setDeptMemberPageSize,
      setAddDeptMemberOpen,
      setDeptCandidateKeyword,
      setSelectedCandidateIds,
      setDeptLeaderOpen,
      setDeptLeaderUserId,
      setDeptConfirmOpen,
    },
  }
}
