import { useMemo, useState } from 'react'
import { ChevronDown, Folder, FolderTree, Loader, Search } from 'lucide-react'
import type { DepartmentNode } from '../../../types/department'

type DepartmentTreeProps = {
  tree: DepartmentNode[]
  selectedId: number | null
  loading: boolean
  onSelect: (id: number) => void
  onCreateChild: (parentId: number) => void
}

export function DepartmentTree({ tree, selectedId, loading, onSelect, onCreateChild }: DepartmentTreeProps) {
  const [keyword, setKeyword] = useState('')
  const searchKeyword = keyword.trim().toLowerCase()
  const visibleTree = useMemo(() => filterDepartmentTree(tree, searchKeyword), [tree, searchKeyword])
  const firstMatchedDepartmentId = useMemo(() => findFirstDepartmentId(visibleTree), [visibleTree])

  return (
    <section className="org-card">
      <div className="org-card-head">
        <div className="org-card-title"><FolderTree size={16} /> 组织树</div>
      </div>
      <div className="org-card-body org-tree-body">
        <div className="org-search-row">
          <input
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索部门 / 负责人"
            type="search"
            value={keyword}
          />
          <button
            disabled={!searchKeyword || firstMatchedDepartmentId === null}
            onClick={() => {
              if (firstMatchedDepartmentId !== null) onSelect(firstMatchedDepartmentId)
            }}
            type="button"
          >
            <Search size={15} />
            定位
          </button>
        </div>
        {loading ? (
          <div className="org-loading">
            <Loader className="spin-icon" size={24} />
            <p>加载中...</p>
          </div>
        ) : visibleTree.length === 0 ? (
          <div className="org-empty">
            <FolderTree size={32} />
            <p>未找到匹配部门</p>
          </div>
        ) : (
          <div className="dept-tree">
            {visibleTree.map((node) => (
              <DepartmentTreeNode
                depth={0}
                forceExpanded={Boolean(searchKeyword)}
                key={node.id}
                node={node}
                onCreateChild={onCreateChild}
                onSelect={onSelect}
                selectedId={selectedId}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

type DepartmentTreeNodeProps = {
  node: DepartmentNode
  selectedId: number | null
  onSelect: (id: number) => void
  onCreateChild: (parentId: number) => void
  depth: number
  forceExpanded: boolean
}

function DepartmentTreeNode({ node, selectedId, onSelect, onCreateChild, depth, forceExpanded }: DepartmentTreeNodeProps) {
  const [expanded, setExpanded] = useState(true)
  const hasChildren = node.children.length > 0
  const isExpanded = forceExpanded || expanded

  return (
    <div className="tree-node-wrapper">
      <div
        className={`tree-node-row${selectedId === node.id ? ' selected' : ''}`}
        onClick={() => onSelect(node.id)}
        role="button"
        style={{ paddingLeft: 16 + depth * 20 }}
        tabIndex={0}
        onKeyDown={(event) => {
          if (event.key === 'Enter') onSelect(node.id)
        }}
      >
        <span
          className={`tree-arrow${expanded ? ' expanded' : ''}`}
          onClick={(event) => {
            event.stopPropagation()
            setExpanded(!expanded)
          }}
          style={{ visibility: hasChildren ? 'visible' : 'hidden' }}
        >
          <ChevronDown size={12} />
        </span>
        <Folder size={14} style={{ color: '#f59e0b', flexShrink: 0 }} />
        <span className="tree-node-label">{node.deptName}</span>
        {node.memberCount > 0 && <span className="tree-node-count">{node.memberCount} 人</span>}
        <span
          className="tree-add-child"
          onClick={(event) => {
            event.stopPropagation()
            onCreateChild(node.id)
          }}
          title="新建子部门"
        >
          +
        </span>
      </div>
      {hasChildren && isExpanded && (
        <div>
          {node.children.map((child) => (
            <DepartmentTreeNode
              depth={depth + 1}
              forceExpanded={forceExpanded}
              key={child.id}
              node={child}
              onCreateChild={onCreateChild}
              onSelect={onSelect}
              selectedId={selectedId}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function filterDepartmentTree(nodes: DepartmentNode[], keyword: string): DepartmentNode[] {
  if (!keyword) return nodes

  return nodes.reduce<DepartmentNode[]>((result, node) => {
    const matchedChildren = filterDepartmentTree(node.children, keyword)
    const matchedSelf = [node.deptName, node.leaderDisplayName].some((value) => value?.toLowerCase().includes(keyword))

    if (matchedSelf || matchedChildren.length > 0) {
      result.push({ ...node, children: matchedChildren })
    }

    return result
  }, [])
}

function findFirstDepartmentId(nodes: DepartmentNode[]): number | null {
  return nodes[0]?.id ?? null
}
