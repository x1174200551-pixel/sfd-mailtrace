import { useEffect, useMemo, useRef, useState } from 'react'
import { readStoredToken, resolveApiUrl } from '../../shared/api/request'

function buildEmailSrcDoc(html: string, compactParagraphMargins: boolean): string {
  const baseHref = typeof window === 'undefined' ? '/' : `${window.location.origin}/`
  const normalizedHtml = compactParagraphMargins
    ? html.replace(
        /(<\/(?:p|div|ul|ol|blockquote|h[1-6]|table)>)\s*(<br\s*\/?>)\s*<br\s*\/?>/gi,
        '$1$2',
      )
    : html
  const paragraphStyle = compactParagraphMargins
    ? `
      p { margin-block: 0; }
      p + p { margin-top: 1.7em; }
    `
    : ''
  const baseStyle = `
    <meta charset="utf-8" />
    <base href="${baseHref}" target="_blank" />
    <style>
      html, body { margin: 0; padding: 0; background: #fff; }
      body {
        overflow-wrap: anywhere;
        color: #4b5563;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
        font-size: 13px;
        font-weight: 400;
        line-height: 1.7;
      }
      img { max-width: 100%; height: auto; }
      ${paragraphStyle}
    </style>
  `
  const trimmed = normalizedHtml.trim()
  if (/<html[\s>]/i.test(trimmed)) {
    if (/<head[\s>]/i.test(trimmed)) {
      return trimmed.replace(/<head([^>]*)>/i, `<head$1>${baseStyle}`)
    }
    return trimmed.replace(/<html([^>]*)>/i, `<html$1><head>${baseStyle}</head>`)
  }
  return `<!doctype html><html><head>${baseStyle}</head><body>${trimmed}</body></html>`
}

const API_RESOURCE_URL_RE =
  /(?:https?:\/\/[^"'()<>\s]+)?\/(?:api|mailtrace-api)\/v1\/(?:tickets\/\d+\/attachments\/\d+\/download|customer-tickets\/[^"'()<>\s/]+\/attachments\/\d+\/download\?token=[^"'()<>\s]+)/g

const PROTECTED_INLINE_RESOURCE_URL_RE =
  /(?:https?:\/\/[^"'()<>\s]+)?\/(?:api|mailtrace-api)\/v1\/tickets\/\d+\/attachments\/\d+\/download/g

function toApiResourcePath(url: string) {
  try {
    const parsed = new URL(url, window.location.origin)
    return `${parsed.pathname}${parsed.search}`
  } catch {
    return url
  }
}

function resolveInlineResourceUrl(url: string) {
  const apiPath = toApiResourcePath(url)
  if (apiPath.startsWith('/api/')) {
    return resolveApiUrl(apiPath)
  }
  if (apiPath.startsWith('/mailtrace-api/')) {
    return resolveApiUrl(`/api${apiPath.slice('/mailtrace-api'.length)}`)
  }
  return url
}

function rewriteInlineResourceUrls(html: string) {
  return html.replace(API_RESOURCE_URL_RE, (url) => resolveInlineResourceUrl(url))
}

async function resolveAuthorizedInlineResources(html: string, signal: AbortSignal) {
  const rewrittenHtml = rewriteInlineResourceUrls(html)
  const urls = Array.from(new Set(rewrittenHtml.match(PROTECTED_INLINE_RESOURCE_URL_RE) ?? []))
  const objectUrls: string[] = []
  if (urls.length === 0) {
    return { html: rewrittenHtml, objectUrls }
  }

  const token = readStoredToken()
  if (!token) {
    return { html: rewrittenHtml, objectUrls }
  }

  let nextHtml = rewrittenHtml
  const replacements = await Promise.all(
    urls.map(async (url) => {
      try {
        const response = await fetch(url, {
          headers: { Authorization: `Bearer ${token}` },
          signal,
        })
        if (!response.ok) return null
        const objectUrl = URL.createObjectURL(await response.blob())
        objectUrls.push(objectUrl)
        return { url, objectUrl }
      } catch {
        return null
      }
    }),
  )

  replacements.forEach((item) => {
    if (!item) return
    nextHtml = nextHtml.split(item.url).join(item.objectUrl)
  })
  return { html: nextHtml, objectUrls }
}

type EmailHtmlFrameProps = {
  html: string
  authorizeInlineResources?: boolean
  compactParagraphMargins?: boolean
}

export function EmailHtmlFrame({
  html,
  authorizeInlineResources = true,
  compactParagraphMargins = false,
}: EmailHtmlFrameProps) {
  const iframeRef = useRef<HTMLIFrameElement>(null)
  const [height, setHeight] = useState(180)
  const [resolvedHtml, setResolvedHtml] = useState(html)
  const srcDoc = useMemo(
    () => buildEmailSrcDoc(resolvedHtml, compactParagraphMargins),
    [compactParagraphMargins, resolvedHtml],
  )

  useEffect(() => {
    const controller = new AbortController()
    let mounted = true
    let objectUrls: string[] = []
    setResolvedHtml(rewriteInlineResourceUrls(html))

    if (!authorizeInlineResources) {
      return () => {
        mounted = false
        controller.abort()
      }
    }

    resolveAuthorizedInlineResources(html, controller.signal)
      .then((result) => {
        if (!mounted) {
          result.objectUrls.forEach((objectUrl) => URL.revokeObjectURL(objectUrl))
          return
        }
        objectUrls = result.objectUrls
        setResolvedHtml(result.html)
      })
      .catch(() => {
        if (mounted) setResolvedHtml(rewriteInlineResourceUrls(html))
      })

    return () => {
      mounted = false
      controller.abort()
      objectUrls.forEach((objectUrl) => URL.revokeObjectURL(objectUrl))
    }
  }, [authorizeInlineResources, html])

  const syncHeight = () => {
    const doc = iframeRef.current?.contentDocument
    if (!doc) return
    const nextHeight = Math.max(
      doc.body.scrollHeight,
      doc.body.offsetHeight,
      doc.documentElement.scrollHeight,
      doc.documentElement.offsetHeight,
    )
    setHeight(Math.max(180, nextHeight + 2))
  }

  const handleLoad = () => {
    syncHeight()
    window.setTimeout(syncHeight, 120)
    window.setTimeout(syncHeight, 600)
  }

  return (
    <iframe
      ref={iframeRef}
      className="msg-body-frame"
      title="邮件正文"
      sandbox="allow-same-origin"
      srcDoc={srcDoc}
      style={{ height }}
      onLoad={handleLoad}
    />
  )
}
