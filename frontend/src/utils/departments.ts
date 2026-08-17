import type { DepartmentNode } from '../types/department'

export function flattenDepartments(nodes: DepartmentNode[]): DepartmentNode[] {
  return nodes.flatMap((node) => [node, ...flattenDepartments(node.children || [])])
}

export function countMembers(nodes: DepartmentNode[]): number {
  return nodes.reduce((sum, node) => sum + node.memberCount + countMembers(node.children || []), 0)
}

export function findDeptNode(nodes: DepartmentNode[], id: number): DepartmentNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    const found = findDeptNode(node.children || [], id)
    if (found) return found
  }
  return null
}
