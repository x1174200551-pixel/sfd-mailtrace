import { useState } from 'react'
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
  return (
    <section className="org-card">
      <div className="org-card-head">
        <div className="org-card-title"><FolderTree size={16} /> 组织树</div>
      </div>
      <div className="org-card-body org-tree-body">
        <div className="org-search-row">
          <input placeholder="搜索部门 / 负责人" type="search" />
          <button type="button"><Search size={15} /> 定位</button>
        </div>
        {loading ? (
          <div className="org-loading">
            <Loader className="spin-icon" size={24} />
            <p>加载中...</p>
          </div>
        ) : (
          <div className="dept-tree">
            {tree.map((node) => (
              <DepartmentTreeNode
                depth={0}
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
}

function DepartmentTreeNode({ node, selectedId, onSelect, onCreateChild, depth }: DepartmentTreeNodeProps) {
  const [expanded, setExpanded] = useState(true)
  const hasChildren = node.children.length > 0

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
      {hasChildren && expanded && (
        <div>
          {node.children.map((child) => (
            <DepartmentTreeNode
              depth={depth + 1}
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
