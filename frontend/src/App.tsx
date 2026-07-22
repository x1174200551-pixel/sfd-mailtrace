import { useEffect, useState } from 'react'
import './App.css'

type HealthVO = {
  status: string
  app: string
  version: string
}

type BasicResult<T> = {
  code: number
  message: string
  data: T
}

const menus = [
  '工作台',
  '工单中心',
  '客户与联系人',
  '邮件与邮箱',
  '规则与分配',
  'SLA 与提醒',
  '统计报表',
  '系统管理',
]

function App() {
  const [health, setHealth] = useState<HealthVO | null>(null)
  const [error, setError] = useState<string>('')

  useEffect(() => {
    fetch('/api/v1/system/health')
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json() as Promise<BasicResult<HealthVO>>
      })
      .then((body) => {
        if (body.code !== 0) throw new Error(body.message || '接口失败')
        setHealth(body.data)
      })
      .catch((e: Error) => setError(e.message))
  }, [])

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">MailTrace</div>
        <p className="brand-sub">邮迹工单</p>
        <nav>
          {menus.map((item) => (
            <div key={item} className="menu-item">
              {item}
            </div>
          ))}
        </nav>
      </aside>
      <main className="main">
        <h1>项目骨架已就绪</h1>
        <p>前后端同仓 · `com.sfonda.mailtrace` · 表前缀 `mt_`</p>
        <section className="card">
          <h2>后端健康检查</h2>
          {health ? (
            <ul>
              <li>应用：{health.app}</li>
              <li>状态：{health.status}</li>
              <li>版本：{health.version}</li>
            </ul>
          ) : (
            <p className="muted">{error ? `未连通：${error}` : '检测中…（请先启动后端）'}</p>
          )}
        </section>
      </main>
    </div>
  )
}

export default App
