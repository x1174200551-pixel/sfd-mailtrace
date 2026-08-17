import { useCallback, useEffect, useMemo, useState } from 'react'
import { departmentApi } from '../api/departments'
import type { DepartmentNode } from '../types/department'
import { flattenDepartments } from '../utils/departments'

type UseUserDepartmentOptionsParams = {
  activeMenu: string
  canReadUsers: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useUserDepartmentOptions({
  activeMenu,
  canReadUsers,
  handleAuthExpired,
  token,
}: UseUserDepartmentOptionsParams) {
  const [departmentTree, setDepartmentTree] = useState<DepartmentNode[]>([])
  const [departmentsError, setDepartmentsError] = useState('')
  const departmentOptions = useMemo(
    () => flattenDepartments(departmentTree).filter((department) => department.enabled),
    [departmentTree],
  )

  const fetchDepartments = useCallback(async () => {
    if (!token || activeMenu !== '用户管理' || !canReadUsers) return
    try {
      const data = await departmentApi.list({ enabled: true })
      setDepartmentTree(data)
      setDepartmentsError('')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setDepartmentsError(error instanceof Error ? error.message : '部门列表加载失败')
    }
  }, [activeMenu, canReadUsers, handleAuthExpired, token])

  useEffect(() => {
    void fetchDepartments()
  }, [fetchDepartments])

  return {
    defaultDepartmentId: departmentOptions[0]?.id ?? null,
    departmentOptions,
    departmentsError,
  }
}
