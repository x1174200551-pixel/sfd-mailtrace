import type { FormEvent } from 'react'
import loginCardVisual from '../../assets/login-prototype-card.png'
import loginEnvelopeVisual from '../../assets/login-prototype-envelope.png'

export type LoginModalState = {
  title: string
  text: string
} | null

type LoginPageProps = {
  account: string
  accountError: string
  formError: string
  modal: LoginModalState
  onAccountChange: (value: string) => void
  onModalChange: (modal: LoginModalState) => void
  onPasswordChange: (value: string) => void
  onRememberMeChange: (remember: boolean) => void
  onShowPasswordChange: (updater: (value: boolean) => boolean) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  password: string
  passwordError: string
  rememberMe: boolean
  showPassword: boolean
  submitting: boolean
}

export function LoginPage({
  account,
  accountError,
  formError,
  modal,
  onAccountChange,
  onModalChange,
  onPasswordChange,
  onRememberMeChange,
  onShowPasswordChange,
  onSubmit,
  password,
  passwordError,
  rememberMe,
  showPassword,
  submitting,
}: LoginPageProps) {
  const toastMessage = formError || accountError || passwordError || '原型演示'
  const showToast = Boolean(formError || accountError || passwordError)

  return (
    <main className="login-page">
      <div className={`toast${showToast ? ' show' : ''}`} role={showToast ? 'alert' : undefined}>
        {toastMessage}
      </div>

      <div className="top-wave"></div>
      <div className="bottom-wave"></div>

      <div className="brand">
        <div className="brand-logo">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M3 6.8 12 13l9-6.2M4.5 5h15A1.5 1.5 0 0 1 21 6.5v11A1.5 1.5 0 0 1 19.5 19h-15A1.5 1.5 0 0 1 3 17.5v-11A1.5 1.5 0 0 1 4.5 5Z"
              stroke="white"
              strokeLinejoin="round"
              strokeWidth="1.8"
            />
          </svg>
        </div>
        <div>
          <div className="brand-title">邮件工单系统</div>
          <div className="brand-sub">高效连接 · 智能服务 · 价值驱动</div>
        </div>
      </div>

      <div className="slogan">
        <div className="slogan-main">
          让每一封邮件
          <span className="slogan-accent">都有回应</span>
        </div>
        <div className="slogan-sub">智能收件 · 工单管理 · 团队协作 · 服务升级</div>
      </div>

      <div className="features">
        <div className="feature mail">
          <div className="fi">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M3 7l9 6 9-6M4.5 5h15A1.5 1.5 0 0 1 21 6.5v11A1.5 1.5 0 0 1 19.5 19h-15A1.5 1.5 0 0 1 3 17.5v-11A1.5 1.5 0 0 1 4.5 5Z"
                strokeWidth="1.7"
              />
            </svg>
          </div>
          <div>
            <b>智能收件</b>
            <span>多渠道接入，自动识别</span>
          </div>
        </div>
        <div className="feature chart">
          <div className="fi">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M5 20V11M12 20V4M19 20v-7" strokeLinecap="round" strokeWidth="2" />
            </svg>
          </div>
          <div>
            <b>数据洞察</b>
            <span>多维度统计，洞察趋势</span>
          </div>
        </div>
        <div className="feature team">
          <div className="fi">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="9" cy="8" r="3" />
              <circle cx="16.5" cy="9" r="2.5" />
              <path d="M3.5 20a5.5 5.5 0 0 1 11 0M13 20a4.5 4.5 0 0 1 8 0" strokeWidth="1.7" />
            </svg>
          </div>
          <div>
            <b>团队协作</b>
            <span>高效分配，协同处理</span>
          </div>
        </div>
      </div>

      <div className="notice">
        <strong>🔔 及时通知</strong>
        <small>状态变更，实时提醒</small>
      </div>

      <div className="hero">
        <img src={loginCardVisual} alt="邮件工单系统主视觉" />
      </div>

      <div className="capbar">
        <div className="cap">
          <div className="cap-i">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="m13 2-7 11h5l-1 9 8-12h-5V2Z" strokeLinejoin="round" strokeWidth="1.8" />
            </svg>
          </div>
          <div>
            <b>智能路由</b>
            <span>
              自动识别规则
              <br />
              精准分配工单
            </span>
          </div>
        </div>
        <div className="cap">
          <div className="cap-i">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="12" r="8" />
              <path d="M12 7v5l3 2" strokeLinecap="round" strokeWidth="1.8" />
            </svg>
          </div>
          <div>
            <b>SLA 监控</b>
            <span>
              全程跟踪监控
              <br />
              保障服务时效
            </span>
          </div>
        </div>
        <div className="cap">
          <div className="cap-i">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="m4 7 8-4 8 4-8 4-8-4Zm0 0v9l8 4 8-4V7M12 11v9" strokeWidth="1.8" />
            </svg>
          </div>
          <div>
            <b>知识沉淀</b>
            <span>
              知识库集中管理
              <br />
              经验持续复用
            </span>
          </div>
        </div>
        <div className="cap">
          <div className="cap-i">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M12 3 5 6v5c0 4.4 2.8 7.7 7 10 4.2-2.3 7-5.6 7-10V6l-7-3Z" strokeWidth="1.8" />
              <path d="m9 12 2 2 4-4" strokeLinecap="round" strokeWidth="1.8" />
            </svg>
          </div>
          <div>
            <b>安全可靠</b>
            <span>
              权限精细管控
              <br />
              数据安全保障
            </span>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="top-visual">
          <img src={loginEnvelopeVisual} alt="登录信封" />
        </div>
        <h1 className="title">欢迎回来！</h1>
        <div className="sub">登录邮件工单系统，开启高效工作之旅</div>
        <form onSubmit={onSubmit} noValidate>
          <div className="form-item">
            <span className="icon">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="12" cy="8" r="4" strokeWidth="1.7" />
                <path d="M4.5 20a7.5 7.5 0 0 1 15 0" strokeWidth="1.7" />
              </svg>
            </span>
            <input
              aria-invalid={Boolean(accountError)}
              autoComplete="username"
              className={accountError ? 'invalid' : ''}
              disabled={submitting}
              onChange={(event) => onAccountChange(event.target.value)}
              placeholder="请输入用户名"
              type="text"
              value={account}
            />
          </div>
          <div className="form-item">
            <span className="icon">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <rect x="5" y="10" width="14" height="10" rx="2" strokeWidth="1.7" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3" strokeWidth="1.7" />
              </svg>
            </span>
            <input
              aria-invalid={Boolean(passwordError)}
              autoComplete="current-password"
              className={passwordError ? 'invalid' : ''}
              disabled={submitting}
              onChange={(event) => onPasswordChange(event.target.value)}
              placeholder="请输入密码"
              type={showPassword ? 'text' : 'password'}
              value={password}
            />
            <button
              aria-label={showPassword ? '隐藏密码' : '显示密码'}
              className="eye"
              disabled={submitting}
              onClick={() => onShowPasswordChange((value) => !value)}
              title="显示/隐藏密码"
              type="button"
            >
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M3 12s3.2-5 9-5 9 5 9 5-3.2 5-9 5-9-5-9-5Z" strokeWidth="1.6" />
                <circle cx="12" cy="12" r="2.5" strokeWidth="1.6" />
              </svg>
            </button>
          </div>

          <div className="form-row">
            <label className="remember">
              <input
                checked={rememberMe}
                disabled={submitting}
                onChange={(event) => onRememberMeChange(event.target.checked)}
                type="checkbox"
              />
              记住我
            </label>
            <button
              className="forgot"
              onClick={() =>
                onModalChange({
                  title: '忘记密码',
                  text: '请联系系统管理员重置密码。为保护邮箱与工单数据安全，第一版不开放自助找回入口。',
                })}
              type="button"
            >
              忘记密码？
            </button>
          </div>

          <button className="login-btn" disabled={submitting} type="submit">
            {submitting ? '登录中...' : '登录'}
          </button>
        </form>
        <div className="safe-wrap">
          <div className="divider"></div>
          <div className="safe">
            <span className="shield">◇</span>
            安全登录 · 数据加密 · 稳定可靠
          </div>
        </div>
      </div>

      {modal && (
        <div className="modal-mask" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <div className="modal">
            <h3 id="modal-title">{modal.title}</h3>
            <p>{modal.text}</p>
            <div className="modal-actions">
              <button type="button" onClick={() => onModalChange(null)}>
                取消
              </button>
              <button className="primary" type="button" onClick={() => onModalChange(null)}>
                我知道了
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
