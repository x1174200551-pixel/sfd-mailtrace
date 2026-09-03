import { useMemo, useState } from 'react'
import { Alert, Button, Input, Modal, Spin, Tag } from 'antd'
import dayjs from 'dayjs'
import { CheckCircle2, Clock3, Mail, RefreshCw } from 'lucide-react'
import { customerTicketApi } from '../../api/customer-tickets'
import { BrandLogo } from '../../components/branding/BrandLogo'
import { EmailHtmlFrame } from '../../components/ticket/EmailHtmlFrame'
import {
  CUSTOMER_TICKET_LANGUAGE_STORAGE_KEY,
  createCustomerTicketTranslator,
  customerTicketLanguageOptions,
  resolveCustomerTicketLanguage,
  type CustomerTicketLanguage,
  type CustomerTicketTranslator,
} from '../../constants/customer-ticket-i18n'
import { ApiError } from '../../shared/api/error-handler'
import type { CustomerTicketDetail, CustomerTicketTimelineItem } from '../../types/customer-ticket'

type CustomerTicketLookupPageProps = {
  ticketNo: string
}

type CustomerProgressState = 'done' | 'current' | 'pending' | 'stopped'

type CustomerProgressItem = {
  key: string
  label: string
  state: CustomerProgressState
  time: string
}

function formatDateTime(value?: string | null) {
  if (!value) return '-'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : value
}

function formatShortTime(value?: string | null) {
  if (!value) return '-'
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('MM-DD HH:mm') : value
}

function statusTone(status?: string) {
  if (status === 'CLOSED') return 'success'
  if (status === 'CANCELLED') return 'default'
  if (status === 'WAITING_CUSTOMER') return 'warning'
  return 'processing'
}

function displayStatusLabel(status: string | undefined, t: CustomerTicketTranslator) {
  return status ? t(`status.${status}`) : t('status.PENDING_VERIFY')
}

function CustomerEmailBody({ detail, t }: { detail: CustomerTicketDetail | null; t: CustomerTicketTranslator }) {
  const html = detail?.email?.contentHtml?.trim()
  if (html) {
    return <EmailHtmlFrame html={html} authorizeInlineResources={false} />
  }
  return <div className="customer-email-text">{detail?.email?.contentText || t('email.empty')}</div>
}

function formatDuration(from: string | null | undefined, t: CustomerTicketTranslator, to?: string | null) {
  if (!from) return '-'
  const start = dayjs(from)
  const end = to ? dayjs(to) : dayjs()
  if (!start.isValid() || !end.isValid()) return '-'
  const minutes = Math.max(0, end.diff(start, 'minute'))
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  const mins = minutes % 60
  if (days > 0) return t('time.dayHour', { days, hours })
  if (hours > 0) return t('time.hourMinute', { hours, minutes: mins })
  return t('time.minute', { minutes: mins })
}

function formatRemaining(deadline: string | null | undefined, t: CustomerTicketTranslator) {
  if (!deadline) return '-'
  const end = dayjs(deadline)
  if (!end.isValid()) return '-'
  const diff = end.diff(dayjs(), 'minute')
  if (diff >= 0) {
    return t('sla.remaining', { duration: formatDuration(dayjs().toISOString(), t, end.toISOString()) })
  }
  return t('sla.overdue', { duration: formatDuration(end.toISOString(), t, dayjs().toISOString()) })
}

function progressPercent(from?: string | null, deadline?: string | null, finish?: string | null) {
  if (!from || !deadline) return 0
  const start = dayjs(from)
  const end = dayjs(deadline)
  const current = finish ? dayjs(finish) : dayjs()
  if (!start.isValid() || !end.isValid() || !current.isValid()) return 0
  const total = Math.max(1, end.diff(start, 'minute'))
  const used = Math.max(0, current.diff(start, 'minute'))
  return Math.min(100, Math.round((used / total) * 100))
}

function timelineStageSet(timeline: CustomerTicketTimelineItem[]) {
  return new Set(timeline.map(item => item.stage))
}

function timelineTime(timeline: CustomerTicketTimelineItem[], stage: string) {
  return timeline.find(item => item.stage === stage)?.eventAt || null
}

function buildProgressItems(detail: CustomerTicketDetail | null, stages: Set<string>, t: CustomerTicketTranslator): CustomerProgressItem[] {
  if (!detail) {
    return [
      { key: 'RECEIVED', label: t('progress.received'), state: 'pending', time: '-' },
      { key: 'PROCESSING', label: t('progress.processing'), state: 'pending', time: '-' },
      { key: 'FIRST_REPLY', label: t('progress.firstReply'), state: 'pending', time: '-' },
      { key: 'RESOLVE', label: t('progress.resolve'), state: 'pending', time: '-' },
      { key: 'CLOSED', label: t('progress.closed'), state: 'pending', time: '-' },
    ]
  }

  const isClosed = detail.status === 'CLOSED'
  const isCancelled = detail.status === 'CANCELLED'
  const isPendingAssign = detail.status === 'PENDING_ASSIGN'
  const hasFirstReply = Boolean(detail.firstReplyAt)
  const processingStarted = stages.has('PROCESSING') || detail.status !== 'PENDING_ASSIGN'
  const processingCurrent = processingStarted && !hasFirstReply && !isClosed && !isCancelled
  const resolveCurrent = hasFirstReply && !isClosed && !isCancelled
  const resolveLabel = detail.status === 'WAITING_CUSTOMER' ? t('progress.waitingCustomer') : t('progress.resolve')

  return [
    {
      key: 'RECEIVED',
      label: t('progress.received'),
      state: isPendingAssign ? 'current' : 'done',
      time: formatShortTime(detail.createdAt),
    },
    {
      key: 'PROCESSING',
      label: t('progress.processing'),
      state: processingCurrent ? 'current' : processingStarted ? 'done' : 'pending',
      time: formatShortTime(timelineTime(detail.timeline, 'PROCESSING') || detail.createdAt),
    },
    {
      key: 'FIRST_REPLY',
      label: t('progress.firstReply'),
      state: hasFirstReply ? 'done' : 'pending',
      time: hasFirstReply ? formatShortTime(detail.firstReplyAt) : t('sla.deadlinePrefix', { time: formatShortTime(detail.slaResponseDeadline) }),
    },
    {
      key: 'RESOLVE',
      label: resolveLabel,
      state: isClosed ? 'done' : isCancelled ? 'stopped' : resolveCurrent ? 'current' : 'pending',
      time: resolveCurrent ? displayStatusLabel(detail.status, t) : t('sla.resolveDeadline', { time: formatShortTime(detail.slaResolveDeadline) }),
    },
    {
      key: 'CLOSED',
      label: isCancelled ? t('progress.cancelled') : t('progress.closed'),
      state: isClosed ? 'done' : isCancelled ? 'stopped' : 'pending',
      time: isClosed ? formatShortTime(detail.closedAt) : '-',
    },
  ]
}

function timelineDisplay(item: CustomerTicketTimelineItem, ticketNo: string, t: CustomerTicketTranslator) {
  const title = t(`timeline.${item.stage}.title`)
  const content = t(`timeline.${item.stage}.content`, { ticketNo })
  const badge = item.stage === 'AGENT_REPLY' || item.stage === 'FIRST_REPLY' ? t('timeline.badge.service') : t('timeline.badge.system')
  return {
    title: title === `timeline.${item.stage}.title` ? item.title : title,
    content: content === `timeline.${item.stage}.content` ? item.content : content,
    badge,
  }
}

function verifyErrorMessage(error: unknown, language: CustomerTicketLanguage, t: CustomerTicketTranslator) {
  if (error instanceof ApiError && language === 'en-US') {
    const messageKey = `error.${error.code || error.status}`
    const translated = t(messageKey)
    return translated === messageKey ? t('error.verifyFallback') : translated
  }
  return error instanceof Error ? error.message : t('error.verifyFallback')
}

export function CustomerTicketLookupPage({ ticketNo }: CustomerTicketLookupPageProps) {
  const [language, setLanguage] = useState<CustomerTicketLanguage>(() => resolveCustomerTicketLanguage())
  const [accessCode, setAccessCode] = useState('')
  const [detail, setDetail] = useState<CustomerTicketDetail | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [closed, setClosed] = useState(false)

  const t = useMemo(() => createCustomerTicketTranslator(language), [language])
  const stages = useMemo(() => timelineStageSet(detail?.timeline || []), [detail])
  const progressItems = useMemo(() => buildProgressItems(detail, stages, t), [detail, stages, t])
  const currentProgress = useMemo(() => {
    if (!detail) return ''
    if (detail.status === 'CLOSED') return t('current.closed')
    if (detail.status === 'CANCELLED') return t('current.cancelled')
    if (detail.status === 'WAITING_CUSTOMER') return t('current.waitingCustomer')
    if (detail.slaResolveDeadline) return t('current.processingWithDeadline', { time: formatDateTime(detail.slaResolveDeadline) })
    return t('current.processing')
  }, [detail, t])
  const responseSla = useMemo(() => {
    if (!detail) return { label: '-', duration: '-', remain: '-', percent: 0, danger: false }
    const duration = detail.firstReplyAt
      ? t('sla.responseDuration', { duration: formatDuration(detail.createdAt, t, detail.firstReplyAt) })
      : t('sla.waitingDuration', { duration: formatDuration(detail.createdAt, t) })
    const remain = detail.firstReplyAt
      ? t('sla.responseDone')
      : formatRemaining(detail.slaResponseDeadline, t)
    return {
      label: detail.firstReplyAt ? t('sla.responded') : t('sla.pendingResponse'),
      duration,
      remain,
      percent: progressPercent(detail.createdAt, detail.slaResponseDeadline, detail.firstReplyAt),
      danger: !detail.firstReplyAt && !!detail.slaResponseDeadline && dayjs().isAfter(dayjs(detail.slaResponseDeadline)),
    }
  }, [detail, t])
  const resolveSla = useMemo(() => {
    if (!detail) return { duration: '-', remain: '-', percent: 0, danger: false }
    const finishAt = detail.closedAt || null
    return {
      duration: finishAt
        ? t('sla.resolveDuration', { duration: formatDuration(detail.createdAt, t, finishAt) })
        : t('sla.usedDuration', { duration: formatDuration(detail.createdAt, t) }),
      remain: finishAt ? t('sla.resolved') : formatRemaining(detail.slaResolveDeadline, t),
      percent: progressPercent(detail.createdAt, detail.slaResolveDeadline, finishAt),
      danger: !finishAt && !!detail.slaResolveDeadline && dayjs().isAfter(dayjs(detail.slaResolveDeadline)),
    }
  }, [detail, t])

  const switchLanguage = (nextLanguage: CustomerTicketLanguage) => {
    setLanguage(nextLanguage)
    setError('')
    window.localStorage.setItem(CUSTOMER_TICKET_LANGUAGE_STORAGE_KEY, nextLanguage)
    const url = new URL(window.location.href)
    url.searchParams.set('lang', nextLanguage)
    window.history.replaceState(null, '', url.toString())
  }

  const verify = async () => {
    const code = accessCode.trim()
    if (!code) {
      setError(t('error.requiredCode'))
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const data = await customerTicketApi.verify(ticketNo, code)
      setDetail(data)
    } catch (err) {
      setError(verifyErrorMessage(err, language, t))
    } finally {
      setSubmitting(false)
    }
  }

  const closePage = () => {
    window.close()
    setClosed(true)
  }

  const refresh = () => {
    if (!accessCode.trim()) {
      setDetail(null)
      return
    }
    void verify()
  }

  return (
    <main className="customer-ticket-page">
      {closed ? (
        <section className="customer-closed-card">
          <BrandLogo className="customer-brand-mark" />
          <h1>{t('closed.title')}</h1>
          <p>{t('closed.description')}</p>
        </section>
      ) : (
        <>
          <div className={`customer-ticket-shell${detail ? '' : ' is-masked'}`}>
            <header className="customer-topbar">
              <div className="customer-brand">
                <BrandLogo className="customer-brand-mark" />
                <strong>{t('brand.name')}</strong>
              </div>
              <div className="customer-top-actions">
                <div className="customer-topmeta">
                  {t('top.queryTime')}：{dayjs().format('YYYY-MM-DD HH:mm')}
                  {detail?.customerAccessExpiresAt ? (
                    <> · {t('field.tokenExpiresAt')}：<span>{formatDateTime(detail.customerAccessExpiresAt)}</span></>
                  ) : null}
                </div>
                <div className="customer-language-switch" aria-label="Language">
                  {customerTicketLanguageOptions.map(option => (
                    <button
                      className={option.value === language ? 'active' : ''}
                      key={option.value}
                      onClick={() => switchLanguage(option.value)}
                      type="button"
                    >
                      {option.value === 'zh-CN' ? t('language.zh') : t('language.en')}
                    </button>
                  ))}
                </div>
              </div>
            </header>

        <section className="customer-ticket-hero">
          <div>
            <div className="customer-ticket-no">{t('field.ticketNo')}：{detail?.ticketNo || ticketNo}</div>
            <h1>{detail?.subject || t('title.fallback')}</h1>
          </div>
          <div className="customer-ticket-hero-actions">
            <Tag color={statusTone(detail?.status)} className="customer-status-tag">
              {displayStatusLabel(detail?.status, t)}
            </Tag>
            <Button type="primary" icon={<RefreshCw size={16} />} onClick={refresh} disabled={!detail} loading={submitting}>
              {t('action.refreshProgress')}
            </Button>
          </div>
        </section>

        <section className="customer-meta-grid">
          <div><span>{t('field.customerEmail')}</span><strong>{detail?.customerEmail || '-'}</strong></div>
          <div><span>{t('field.createdAt')}</span><strong>{formatDateTime(detail?.createdAt)}</strong></div>
          <div><span>{t('field.updatedAt')}</span><strong>{formatDateTime(detail?.updatedAt)}</strong></div>
          <div><span>{t('field.firstReply')}</span><strong>{detail?.firstReplyAt ? t('sla.responded') : '-'}</strong></div>
          <div><span>{t('field.slaStatus')}</span><strong className={detail?.slaBreached ? 'danger' : 'success'}>{detail ? (detail.slaBreached ? t('sla.breached') : t('sla.normal')) : '-'}</strong></div>
          <div><span>{t('field.firstReplyDeadline')}</span><strong>{formatDateTime(detail?.slaResponseDeadline)}</strong></div>
          <div><span>{t('field.resolveDeadline')}</span><strong>{formatDateTime(detail?.slaResolveDeadline)}</strong></div>
          <div><span>{t('field.tokenExpiresAt')}</span><strong>{formatDateTime(detail?.customerAccessExpiresAt)}</strong></div>
        </section>

        <section className="customer-card customer-progress-card">
          <h2>{t('section.progress')}</h2>
          <div className="customer-progress-steps">
            {progressItems.map((step, index) => {
              return (
                <div className={`customer-progress-step ${step.state}`} key={step.key}>
                  <div className="customer-step-dot">{step.state === 'done' ? <CheckCircle2 size={16} /> : step.state === 'current' ? '●' : index + 1}</div>
                  <strong>{step.label}</strong>
                  <span>{step.time}</span>
                </div>
              )
            })}
          </div>
        </section>

        <div className="customer-content-grid">
          <div className="customer-main-column">
            <section className="customer-card">
              <h2>{t('section.timeline')}</h2>
              <div className="customer-timeline">
                {(detail?.timeline || []).map((item, index) => {
                  const display = timelineDisplay(item, detail?.ticketNo || ticketNo, t)
                  return (
                    <div className="customer-timeline-item" key={`${item.stage}-${item.eventAt || index}`}>
                      <i />
                      <div className="customer-timeline-body">
                        <div className="customer-timeline-head">
                          <strong>{display.title}</strong>
                          <span>{formatDateTime(item.eventAt)}</span>
                        </div>
                        <p>{display.content}</p>
                        <em>{display.badge}</em>
                      </div>
                    </div>
                  )
                })}
              </div>
            </section>

            <section className="customer-card">
              <h2><Mail size={18} /> {t('section.email')}</h2>
              <div className="customer-email-box">
                <p><strong>{t('field.sender')}：</strong>{detail?.email?.fromAddress || '-'}</p>
                <p><strong>{t('field.sentAt')}：</strong>{formatDateTime(detail?.email?.sentAt)}</p>
                <p><strong>{t('field.subject')}：</strong>{detail?.email?.subject || detail?.subject || '-'}</p>
                <CustomerEmailBody detail={detail} t={t} />
              </div>
            </section>
          </div>

          <aside className="customer-side-column">
            <section className="customer-card customer-side-card">
              <h2>{t('section.sla')}</h2>
              <div className="customer-sla-block">
                <div className="customer-side-row">
                  <span>{t('sla.response')}</span>
                  <strong className={responseSla.danger ? 'danger' : ''}>{responseSla.label}</strong>
                </div>
                <div className="customer-sla-meta">
                  <span>{responseSla.duration}</span>
                  <span className={responseSla.danger ? 'danger' : ''}>{responseSla.remain}</span>
                </div>
                <div className="customer-sla-track">
                  <i className={responseSla.danger ? 'danger' : ''} style={{ width: `${responseSla.percent}%` }} />
                </div>
                <div className="customer-sla-deadline">{t('sla.firstReplyDeadline', { time: formatShortTime(detail?.slaResponseDeadline) })}</div>
              </div>

              <div className="customer-sla-block">
                <div className="customer-side-row">
                  <span>{t('sla.resolve')}</span>
                  <strong className={resolveSla.danger ? 'danger' : ''}>{detail?.closedAt ? t('sla.done') : t('sla.inProgress')}</strong>
                </div>
                <div className="customer-sla-meta">
                  <span>{resolveSla.duration}</span>
                  <span className={resolveSla.danger ? 'danger' : ''}>{resolveSla.remain}</span>
                </div>
                <div className="customer-sla-track">
                  <i className={resolveSla.danger ? 'danger' : 'warning'} style={{ width: `${resolveSla.percent}%` }} />
                </div>
                <div className="customer-sla-deadline">{t('sla.resolveDeadline', { time: formatShortTime(detail?.slaResolveDeadline) })}</div>
              </div>
            </section>

            <section className="customer-card customer-side-card">
              <h2>{t('section.ticket')}</h2>
              <div className="customer-side-row"><span>{t('field.currentStatus')}</span><strong>{detail ? displayStatusLabel(detail.status, t) : '-'}</strong></div>
              <div className="customer-side-row"><span>{t('field.sourceChannel')}</span><strong>{t('source.email')}</strong></div>
              <div className="customer-side-row"><span>{t('field.tokenExpiresAt')}</span><strong>{formatDateTime(detail?.customerAccessExpiresAt)}</strong></div>
            </section>

            <section className="customer-card customer-side-card">
              <h2><Clock3 size={18} /> {t('section.latestProgress')}</h2>
              <p className="customer-latest-progress">{currentProgress || t('latest.empty')}</p>
            </section>
          </aside>
        </div>
      </div>

      {!detail ? (
        <Modal
          centered
          closable={false}
          footer={null}
          maskClosable={false}
          open
          width={420}
          className="customer-verify-modal"
        >
          <div className="customer-verify-content">
            <BrandLogo className="customer-verify-icon" />
            <h2>{t('modal.title')}</h2>
            <p>{t('modal.description')}</p>
            <label>
              <span>{t('field.ticketNo')}</span>
              <Input value={ticketNo} disabled size="large" />
            </label>
            <label>
              <span>{t('modal.accessCode')}</span>
              <Input
                autoFocus
                maxLength={8}
                onChange={event => setAccessCode(event.target.value)}
                onPressEnter={verify}
                placeholder={t('modal.accessCodePlaceholder')}
                size="large"
                value={accessCode}
              />
            </label>
            {error ? <Alert showIcon type="error" title={error} /> : null}
            <div className="customer-verify-actions">
              <Button size="large" onClick={closePage}>{t('action.closePage')}</Button>
              <Button type="primary" size="large" loading={submitting} onClick={verify}>
                {t('action.confirmView')}
              </Button>
            </div>
          </div>
        </Modal>
      ) : null}

          {submitting && detail ? (
            <div className="customer-refresh-mask">
              <Spin />
            </div>
          ) : null}
        </>
      )}
    </main>
  )
}
