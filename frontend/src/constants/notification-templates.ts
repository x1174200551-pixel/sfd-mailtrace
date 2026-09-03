import type { TemplateFormState } from '../types/notification-template'

export const emptyTemplateForm: TemplateFormState = {
  id: null,
  templateCode: '',
  templateType: 'AUTO_REPLY',
  templateName: '',
  subjectTpl: 'Re: {subject}',
  contentTpl: '',
  enabled: true,
}

export function isThreadedReplyTemplate(templateType: string) {
  return templateType === 'AUTO_REPLY' || templateType === 'AGENT_REPLY'
}

export function normalizedTemplateSubject(templateType: string, subjectTpl: string) {
  return isThreadedReplyTemplate(templateType) ? 'Re: {subject}' : subjectTpl
}

export const templateScenes: Record<string, string> = {
  AUTO_REPLY: '客户来信自动建单',
  ASSIGN_NOTIFY: '工单分配处理人',
  AGENT_REPLY: '处理人回复客户',
  SLA_WARNING: 'SLA 即将超时',
  SLA_BREACH: 'SLA 已超时',
  SYSTEM: '系统通知',
}

export function templateSceneLabel(templateCode: string) {
  return templateScenes[templateCode] || '自定义通知场景'
}
