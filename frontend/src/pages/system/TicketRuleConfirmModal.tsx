import type { TicketNumberRule, TicketRuleFormState } from '../../types/system-config'

type TicketRuleConfirmModalProps = {
  onCancel: () => void
  onConfirm: () => void
  rule: TicketNumberRule | null
  saving: boolean
  ticketRuleForm: TicketRuleFormState
}

export function TicketRuleConfirmModal({
  onCancel,
  onConfirm,
  rule,
  saving,
  ticketRuleForm,
}: TicketRuleConfirmModalProps) {
  return (
    <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="ticket-rule-confirm-title">
      <div className="confirm-modal">
        <h3 id="ticket-rule-confirm-title">保存编号规则确认</h3>
        <p>保存后系统会用新规则生成后续工单号，历史工单号不受影响。请确认规则预览无误。</p>
        <div className="confirm-target">
          <strong>{rule?.nextTicketNo || `${ticketRuleForm.prefix || 'TCK'} 规则`}</strong>
          <span>
            {`日期格式：${ticketRuleForm.dateFormat}；随机数位数：${ticketRuleForm.seqLength}；分隔符：${ticketRuleForm.separator || '无'}`}
          </span>
          <span>影响范围：后续客户来信自动建单、自动回执、主题工单号匹配。</span>
        </div>
        <div className="user-modal__foot">
          <button disabled={saving} onClick={onCancel} type="button">
            取消
          </button>
          <button className="primary-action" disabled={saving} onClick={onConfirm} type="button">
            {saving ? '保存中...' : '确认保存'}
          </button>
        </div>
      </div>
    </div>
  )
}
