import { useCallback, useEffect } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import LinkExtension from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import { Button, Tooltip } from 'antd'
import {
  BoldOutlined,
  ItalicOutlined,
  UnderlineOutlined,
  StrikethroughOutlined,
  UnorderedListOutlined,
  OrderedListOutlined,
  LinkOutlined,
  FontSizeOutlined,
  UndoOutlined,
  RedoOutlined,
} from '@ant-design/icons'

interface TiptapRichEditorProps {
  placeholder?: string
  onUpdate?: (html: string, text: string) => void
  initialContent?: string
  disabled?: boolean
}

type ToolbarButton = {
  type?: 'button'
  icon: React.ReactNode
  title: string
  action: () => void
  active: boolean
}

type ToolbarDivider = {
  type: 'divider'
}

type ToolbarItem = ToolbarButton | ToolbarDivider

function MenuBar({ editor, disabled }: { editor: any; disabled?: boolean }) {
  const addLink = useCallback(() => {
    const url = window.prompt('请输入链接地址：')
    if (url) {
      editor.chain().focus().setLink({ href: url }).run()
    }
  }, [editor])

  if (!editor) return null

  const buttons: ToolbarItem[] = [
    { icon: <BoldOutlined />, title: '加粗', action: () => editor.chain().focus().toggleBold().run(), active: editor.isActive('bold') },
    { icon: <ItalicOutlined />, title: '斜体', action: () => editor.chain().focus().toggleItalic().run(), active: editor.isActive('italic') },
    { icon: <UnderlineOutlined />, title: '下划线', action: () => editor.chain().focus().toggleUnderline().run(), active: editor.isActive('underline') },
    { icon: <StrikethroughOutlined />, title: '删除线', action: () => editor.chain().focus().toggleStrike().run(), active: editor.isActive('strike') },
    { type: 'divider' },
    { icon: <FontSizeOutlined />, title: '标题', action: () => editor.chain().focus().toggleHeading({ level: 3 }).run(), active: editor.isActive('heading', { level: 3 }) },
    { icon: <UnorderedListOutlined />, title: '无序列表', action: () => editor.chain().focus().toggleBulletList().run(), active: editor.isActive('bulletList') },
    { icon: <OrderedListOutlined />, title: '有序列表', action: () => editor.chain().focus().toggleOrderedList().run(), active: editor.isActive('orderedList') },
    { type: 'divider' },
    { icon: <LinkOutlined />, title: '插入链接', action: addLink, active: editor.isActive('link') },
    { type: 'divider' },
    { icon: <UndoOutlined />, title: '撤销', action: () => editor.chain().focus().undo().run(), active: false },
    { icon: <RedoOutlined />, title: '重做', action: () => editor.chain().focus().redo().run(), active: false },
  ]

  return (
    <div className="rich-editor-toolbar">
      {buttons.map((btn, i) => {
        if (btn.type === 'divider') {
          return <div key={i} className="rich-editor-divider" />
        }
        return (
          <Tooltip key={i} title={btn.title}>
            <Button
              type="text"
              size="small"
              className={`rich-editor-btn ${btn.active ? 'active' : ''}`}
              disabled={disabled}
              onMouseDown={(e) => { e.preventDefault(); btn.action() }}
              icon={btn.icon}
            />
          </Tooltip>
        )
      })}
    </div>
  )
}

export default function TiptapRichEditor({ placeholder = '请输入内容...', onUpdate, initialContent, disabled = false }: TiptapRichEditorProps) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [3] },
        link: false,
        underline: false,
      }),
      Underline,
      LinkExtension.configure({
        openOnClick: false,
        HTMLAttributes: { target: '_blank', rel: 'noopener noreferrer' },
      }),
      Placeholder.configure({ placeholder }),
    ],
    content: initialContent || '',
    onUpdate: ({ editor }) => {
      const html = editor.getHTML()
      const text = editor.getText()
      onUpdate?.(html, text)
    },
    editorProps: {
      attributes: {
        class: 'rich-editor-content',
      },
    },
    editable: !disabled,
  })

  useEffect(() => {
    editor?.setEditable(!disabled)
  }, [editor, disabled])

  return (
    <div className={`rich-editor-wrapper ${disabled ? 'disabled' : ''}`} onClick={() => { if (!disabled) editor?.chain().focus().run() }}>
      <MenuBar editor={editor} disabled={disabled} />
      <EditorContent editor={editor} />
    </div>
  )
}
