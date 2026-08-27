import { Suspense, lazy } from 'react'
import type { AssignmentRuleTestForm } from '../../types/assignment-rule'

type AppContentModelKey =
  | 'actionLoading'
  | 'activeMailboxStep'
  | 'activeSystemGroup'
  | 'activeSystemGroupConfig'
  | 'assignModalOpen'
  | 'assignNotifyAssignee'
  | 'assignReason'
  | 'assignSending'
  | 'assignUserId'
  | 'assignUsers'
  | 'assignmentActionLoading'
  | 'assignmentEnterpriseOptions'
  | 'assignmentGroupForm'
  | 'assignmentGroupSaving'
  | 'assignmentGroupsData'
  | 'assignmentGroupsLoading'
  | 'assignmentAssigneeOptions'
  | 'assignmentAssignees'
  | 'assignmentConfirmAction'
  | 'assignmentEnabledFilter'
  | 'assignmentForm'
  | 'assignmentKeyword'
  | 'assignmentMailboxOptions'
  | 'assignmentMatchResult'
  | 'assignmentMatchTypeFilter'
  | 'assignmentRuleDirty'
  | 'assignmentRulesData'
  | 'assignmentRulesError'
  | 'assignmentRulesLoading'
  | 'assignmentSaving'
  | 'assignmentTestForm'
  | 'assignmentTesting'
  | 'calendarPreviewCreatedAtValue'
  | 'calendarPreviewResolveHours'
  | 'calendarPreviewResolveHoursValue'
  | 'calendarPreviewResponseHours'
  | 'calendarPreviewResponseHoursValue'
  | 'calendarSlaExample'
  | 'calendarSlaPolicies'
  | 'canClaimCurrentTicket'
  | 'canCreateAssignmentRules'
  | 'canCreateDepartments'
  | 'canCreateEnterprises'
  | 'canCreateHolidays'
  | 'canCreateMailboxes'
  | 'canCreateRoles'
  | 'canCreateSlaPolicies'
  | 'canCreateUsers'
  | 'canCreateWorkCalendars'
  | 'canDeleteHolidays'
  | 'canDeleteMailboxes'
  | 'canDeleteWorkCalendars'
  | 'canEnableDepartments'
  | 'canEnableEnterprises'
  | 'canEnableMailboxes'
  | 'canEnableRoles'
  | 'canEnableUsers'
  | 'canImportHolidays'
  | 'canOpenTicketList'
  | 'canOperateCurrentTicket'
  | 'canReadAssignmentRules'
  | 'canReadCustomers'
  | 'canReadDepartments'
  | 'canReadEnterprises'
  | 'canReadMailboxes'
  | 'canReadRoles'
  | 'canReadSlaPolicies'
  | 'canReadTemplates'
  | 'canReadTicketNumberRule'
  | 'canReadUsers'
  | 'canReadWorkCalendars'
  | 'canResetUserPassword'
  | 'canTestMailboxes'
  | 'canUpdateDepartments'
  | 'canUpdateEnterprises'
  | 'canUpdateHolidays'
  | 'canUpdateMailboxes'
  | 'canUpdateRolePermissions'
  | 'canUpdateRoles'
  | 'canUpdateTicketNumberRule'
  | 'canUpdateUsers'
  | 'changeCustomerKeyword'
  | 'changeCustomerEnterpriseFilter'
  | 'changeCustomerMailboxFilter'
  | 'changeCustomerPage'
  | 'changeDashboardEnterpriseFilter'
  | 'changeFetchLogEnterpriseFilter'
  | 'changeFetchLogMailboxFilter'
  | 'changeFetchLogPage'
  | 'changeFetchLogSuccessFilter'
  | 'changeFetchLogTimeRange'
  | 'changeMailboxKeyword'
  | 'changeMailboxEnterpriseFilter'
  | 'changeMailboxPageSize'
  | 'changeMailboxStatusFilter'
  | 'changeSendLogMailboxFilter'
  | 'changeSendLogEnterpriseFilter'
  | 'changeSendLogPage'
  | 'changeSendLogStatusFilter'
  | 'changeSendLogTimeRange'
  | 'changeSendLogTypeFilter'
  | 'changeTicketKeyword'
  | 'changeTicketEnterpriseFilter'
  | 'changeTicketMailboxFilter'
  | 'changeTicketStatus'
  | 'changeUserEnabledFilter'
  | 'changeUserKeyword'
  | 'changeUserPageSize'
  | 'changeUserRole'
  | 'changeUserRoleFilter'
  | 'claimSending'
  | 'clearFetchLogFilters'
  | 'clearSendLogFilters'
  | 'clearTicketRuleFeedback'
  | 'closeConfirm'
  | 'closeConfirmed'
  | 'closeMailboxConfirm'
  | 'closeModalOpen'
  | 'closeReason'
  | 'closeSending'
  | 'closeTicketRuleConfirm'
  | 'closeUserForm'
  | 'confirmAction'
  | 'customerDetail'
  | 'customerDetailError'
  | 'customerDetailLoading'
  | 'customerEnterpriseFilter'
  | 'customerEnterpriseOptions'
  | 'customerKeyword'
  | 'customerMailboxFilter'
  | 'customerMailboxOptions'
  | 'customerPage'
  | 'customerPageSize'
  | 'customerTicketsData'
  | 'customerTicketsError'
  | 'customerTicketsLoading'
  | 'customersData'
  | 'customersError'
  | 'customersLoading'
  | 'dashboardError'
  | 'dashboardEnterpriseFilter'
  | 'dashboardEnterpriseOptions'
  | 'dashboardLoading'
  | 'dashboardMailboxFilter'
  | 'dashboardMailboxOptions'
  | 'dashboardReport'
  | 'dashboardSummary'
  | 'dashboardTodos'
  | 'dashboardUpdatedAt'
  | 'departmentOptions'
  | 'departmentsError'
  | 'enterpriseActionLoading'
  | 'enterpriseConfirmAction'
  | 'enterpriseEnabledFilter'
  | 'enterpriseForm'
  | 'enterpriseFormOpen'
  | 'enterpriseKeyword'
  | 'enterprisePage'
  | 'enterprisePageSize'
  | 'enterpriseSaving'
  | 'enterprisesData'
  | 'enterprisesError'
  | 'enterprisesLoading'
  | 'fetchAgentUsers'
  | 'fetchAssignmentRules'
  | 'fetchAssignmentGroups'
  | 'fetchCustomerDetail'
  | 'fetchCustomerTickets'
  | 'fetchCustomers'
  | 'fetchDashboard'
  | 'fetchEnterprises'
  | 'fetchHolidays'
  | 'fetchLogDetail'
  | 'fetchLogEnterpriseFilter'
  | 'fetchLogEnterpriseOptions'
  | 'fetchLogMailboxFilter'
  | 'fetchLogMailboxOptions'
  | 'fetchLogPage'
  | 'fetchLogPageSize'
  | 'fetchLogStartFrom'
  | 'fetchLogStartTo'
  | 'fetchLogStats'
  | 'fetchLogSuccessFilter'
  | 'fetchLogsData'
  | 'fetchLogsError'
  | 'fetchLogsLoading'
  | 'fetchMailboxes'
  | 'fetchRoles'
  | 'fetchSlaPolicies'
  | 'fetchTemplates'
  | 'fetchTicketRule'
  | 'fetchUsers'
  | 'fetchWorkCalendarPageAll'
  | 'fetchWorkCalendarsPage'
  | 'getVisibleTicketEvents'
  | 'handleAssign'
  | 'handleAuthExpired'
  | 'handleBackToList'
  | 'handleClaimTicket'
  | 'handleClose'
  | 'handleDeleteAttachment'
  | 'handleOpenDetail'
  | 'handlePriority'
  | 'handleRemoveFile'
  | 'handleReply'
  | 'handleStatusChange'
  | 'handleUploadFile'
  | 'hasPermission'
  | 'holidayDateValue'
  | 'holidayDirty'
  | 'holidayForm'
  | 'holidayImportYear'
  | 'holidayImporting'
  | 'holidayKeyword'
  | 'holidayMonthValue'
  | 'holidayRecords'
  | 'holidaySaving'
  | 'holidaysData'
  | 'holidaysError'
  | 'holidaysLoading'
  | 'importNationalHolidays'
  | 'isAdmin'
  | 'isCurrentTicketTerminal'
  | 'isCurrentTicketUnassigned'
  | 'mailboxActionLoading'
  | 'mailboxAssignees'
  | 'mailboxConfirmAction'
  | 'mailboxDirty'
  | 'mailboxEnterpriseFilter'
  | 'enterpriseOptions'
  | 'mailboxRuleGroupOptions'
  | 'mailboxSlaOptions'
  | 'mailboxTemplateOptions'
  | 'mailboxForm'
  | 'mailboxKeyword'
  | 'mailboxPage'
  | 'mailboxPageSize'
  | 'mailboxSaving'
  | 'mailboxStatusFilter'
  | 'mailboxTestResult'
  | 'mailboxTesting'
  | 'mailboxes'
  | 'mailboxesData'
  | 'mailboxesError'
  | 'mailboxesLoading'
  | 'monthCells'
  | 'moveAssignmentRule'
  | 'moveMailboxStep'
  | 'msgFilter'
  | 'msgSortAsc'
  | 'navigateToTickets'
  | 'openCreateAssignmentRule'
  | 'openCreateAssignmentGroup'
  | 'openCreateEnterprise'
  | 'openCreateHoliday'
  | 'openCreateMailbox'
  | 'openCreateRole'
  | 'openCreateSlaPolicy'
  | 'openCreateTemplate'
  | 'openCreateUser'
  | 'openCreateWorkCalendar'
  | 'openEditUser'
  | 'openEditEnterprise'
  | 'openEnabledConfirm'
  | 'openMailboxConfirm'
  | 'openResetConfirm'
  | 'openTicketFromCustomer'
  | 'openTicketRuleConfirm'
  | 'permissionTree'
  | 'permissionTreeLoading'
  | 'previewTemplate'
  | 'previewTicketRule'
  | 'priorityModalOpen'
  | 'priorityReason'
  | 'prioritySending'
  | 'priorityValue'
  | 'queryFetchLogs'
  | 'querySendLogs'
  | 'refreshFetchLogs'
  | 'refreshSendLogs'
  | 'refreshTickets'
  | 'remarkDraft'
  | 'replyContent'
  | 'replyHtml'
  | 'replySending'
  | 'resetAssignmentFilters'
  | 'resetCalendarPreview'
  | 'resetMailboxFilters'
  | 'resetSlaPolicyFilters'
  | 'resetTicketFilters'
  | 'resetTicketRule'
  | 'resetUserFilters'
  | 'resetWorkCalendarFilters'
  | 'roleDraftMode'
  | 'roleEnabledFilter'
  | 'roleForm'
  | 'roleKeyword'
  | 'rolePermissionSaving'
  | 'roleSaving'
  | 'roleSelectOptions'
  | 'rolesData'
  | 'rolesError'
  | 'rolesLoading'
  | 'runAssignmentRuleTest'
  | 'saveAssignmentRule'
  | 'saveAssignmentGroup'
  | 'saveEnterprise'
  | 'saveHoliday'
  | 'saveMailbox'
  | 'saveSlaPolicy'
  | 'saveTemplate'
  | 'saveTicketRemark'
  | 'saveTicketRule'
  | 'saveWorkCalendar'
  | 'searchCustomers'
  | 'searchTickets'
  | 'selectAssignmentRule'
  | 'selectAssignmentGroup'
  | 'selectedAssignmentEnterpriseId'
  | 'selectedAssignmentGroupId'
  | 'selectHoliday'
  | 'selectMailbox'
  | 'selectRole'
  | 'selectSlaPolicy'
  | 'selectTemplate'
  | 'selectWorkCalendar'
  | 'selectedAssignmentRule'
  | 'selectedCalendarForPage'
  | 'selectedCustomerEmail'
  | 'selectedRole'
  | 'selectedRoleId'
  | 'selectedRoleReadonly'
  | 'selectedSlaPolicy'
  | 'selectedTemplateId'
  | 'selectedWorkCalendar'
  | 'sendLogDetail'
  | 'sendLogEnterpriseFilter'
  | 'sendLogEnterpriseOptions'
  | 'sendLogMailboxFilter'
  | 'sendLogMailboxOptions'
  | 'sendLogPage'
  | 'sendLogPageSize'
  | 'sendLogStartFrom'
  | 'sendLogStartTo'
  | 'sendLogStats'
  | 'sendLogStatusFilter'
  | 'sendLogTypeFilter'
  | 'sendLogsData'
  | 'sendLogsError'
  | 'sendLogsLoading'
  | 'setActiveMailboxStep'
  | 'setActiveMenu'
  | 'setActiveSystemGroup'
  | 'setAssignModalOpen'
  | 'setAssignNotifyAssignee'
  | 'setAssignReason'
  | 'setAssignUserId'
  | 'setAssignmentConfirmAction'
  | 'setAssignmentEnabledFilter'
  | 'setAssignmentKeyword'
  | 'setAssignmentMatchTypeFilter'
  | 'setAssignmentTestForm'
  | 'setAssignmentGroupForm'
  | 'setSelectedAssignmentEnterpriseId'
  | 'setCalendarPreviewCreatedAt'
  | 'setCalendarPreviewResolveHours'
  | 'setCalendarPreviewResponseHours'
  | 'setCloseConfirmed'
  | 'setCloseModalOpen'
  | 'setCloseReason'
  | 'setDashboardMailboxFilter'
  | 'setDefaultSlaPolicy'
  | 'setDefaultWorkCalendar'
  | 'setEnterpriseConfirmAction'
  | 'setEnterpriseEnabledFilter'
  | 'setEnterpriseFormOpen'
  | 'setEnterpriseKeyword'
  | 'setEnterprisePage'
  | 'setEnterprisePageSize'
  | 'setFetchLogDetail'
  | 'setHolidayKeyword'
  | 'setHolidayMonth'
  | 'setMailboxPage'
  | 'setMsgFilter'
  | 'setMsgSortAsc'
  | 'setPriorityModalOpen'
  | 'setPriorityReason'
  | 'setPriorityValue'
  | 'setRemarkDraft'
  | 'setReplyContent'
  | 'setReplyHtml'
  | 'setRoleEnabledFilter'
  | 'setRoleKeyword'
  | 'setSelectedCustomerEmail'
  | 'setSendLogDetail'
  | 'setSlaPolicyConfirmAction'
  | 'setSlaPolicyDefaultFilter'
  | 'setSlaPolicyEnabledFilter'
  | 'setSlaPolicyKeyword'
  | 'setStatusModalOpen'
  | 'setStatusReason'
  | 'setStatusValue'
  | 'setTemplateConfirmOpen'
  | 'setTemplateKeyword'
  | 'setTicketDetailTab'
  | 'setTicketPage'
  | 'setUserPage'
  | 'setWorkCalendarConfirmAction'
  | 'setWorkCalendarDefaultFilter'
  | 'setWorkCalendarKeyword'
  | 'showTicketDetailPage'
  | 'slaCalendarCount'
  | 'slaCalendarOptions'
  | 'slaPoliciesData'
  | 'slaPoliciesError'
  | 'slaPoliciesLoading'
  | 'slaPolicyActionLoading'
  | 'slaPolicyConfirmAction'
  | 'slaPolicyDefaultFilter'
  | 'slaPolicyDirty'
  | 'slaPolicyEnabledFilter'
  | 'slaPolicyForm'
  | 'slaPolicyKeyword'
  | 'slaPolicySaving'
  | 'slaEnterpriseFilter'
  | 'slaEnterpriseOptions'
  | 'slaPreview'
  | 'slaPreviewBaseTime'
  | 'slaResolveHoursInvalid'
  | 'slaWarningInvalid'
  | 'statusModalOpen'
  | 'statusReason'
  | 'statusSending'
  | 'statusValue'
  | 'submitAssignmentConfirm'
  | 'submitEnterpriseConfirm'
  | 'submitConfirmAction'
  | 'submitMailboxConfirm'
  | 'submitRoleBase'
  | 'submitRolePermissions'
  | 'submitSlaPolicyConfirm'
  | 'submitUserForm'
  | 'submitWorkCalendarConfirm'
  | 'systemGroups'
  | 'templateConfirmOpen'
  | 'templateDirty'
  | 'templateForm'
  | 'templateKeyword'
  | 'templatePreview'
  | 'templatePreviewLoading'
  | 'templateSaving'
  | 'templateTypeFilter'
  | 'templatesData'
  | 'templatesError'
  | 'templatesLoading'
  | 'testMailboxConnection'
  | 'ticketAttachments'
  | 'ticketDetail'
  | 'ticketDetailTab'
  | 'ticketEnterpriseFilter'
  | 'ticketEnterpriseOptions'
  | 'ticketKeyword'
  | 'ticketMailboxFilter'
  | 'ticketMailboxOptions'
  | 'ticketPage'
  | 'ticketPageSize'
  | 'ticketRule'
  | 'ticketRuleConfirmOpen'
  | 'ticketRuleDirty'
  | 'ticketRuleError'
  | 'ticketRuleForm'
  | 'ticketRuleLoading'
  | 'ticketRuleMessage'
  | 'ticketRulePreviewLoading'
  | 'ticketRuleSaving'
  | 'ticketSlaBreachedOnly'
  | 'ticketStats'
  | 'ticketStatusTab'
  | 'ticketsData'
  | 'ticketsError'
  | 'ticketsLoading'
  | 'toggleAssignmentRule'
  | 'toggleAssignmentGroup'
  | 'toggleUserGrantEnterprise'
  | 'toggleUserGrantMailbox'
  | 'toggleRoleEnabled'
  | 'toggleRolePermission'
  | 'toggleSlaPolicy'
  | 'updateAssignmentForm'
  | 'updateEnterpriseForm'
  | 'updateHolidayForm'
  | 'updateMailboxForm'
  | 'updateRoleForm'
  | 'updateSlaPolicyForm'
  | 'setSlaEnterpriseFilter'
  | 'updateTemplateForm'
  | 'setTemplateTypeFilter'
  | 'updateTicketRuleForm'
  | 'updateUserForm'
  | 'updateWorkCalendarForm'
  | 'uploadedFiles'
  | 'uploadingFile'
  | 'userEnabledFilter'
  | 'userForm'
  | 'userFormError'
  | 'userFormMode'
  | 'userFormOpen'
  | 'userFormSubmitting'
  | 'userKeyword'
  | 'userPage'
  | 'userPageSize'
  | 'userRoleFilter'
  | 'usersData'
  | 'usersError'
  | 'usersLoading'
  | 'userGrantEnterpriseOptions'
  | 'userGrantForm'
  | 'userGrantLoading'
  | 'userGrantMailboxOptions'
  | 'workCalendarActionLoading'
  | 'workCalendarConfirmAction'
  | 'workCalendarData'
  | 'workCalendarDefaultFilter'
  | 'workCalendarEnterpriseFilter'
  | 'workCalendarEnterpriseOptions'
  | 'workCalendarDirty'
  | 'workCalendarError'
  | 'workCalendarForm'
  | 'workCalendarKeyword'
  | 'workCalendarPageLoading'
  | 'workCalendarSaving'
  | 'workCalendarTimeInvalid'
  | 'setWorkCalendarEnterpriseFilter'
  | 'workCalendars'
  | 'workCalendarsLoading'
  | 'workdayLabel'

export type AppContentModel = Record<AppContentModelKey, any>

const DepartmentManagePage = lazy(() => import('../../pages/system/DepartmentManagePage').then((module) => ({
  default: module.DepartmentManagePage,
})))

const RoleManagePage = lazy(() => import('../../pages/system/RoleManagePage').then((module) => ({
  default: module.RoleManagePage,
})))

const UserManagePage = lazy(() => import('../../pages/system/UserManagePage').then((module) => ({
  default: module.UserManagePage,
})))

const EnterpriseManagePage = lazy(() => import('../../pages/system/EnterpriseManagePage').then((module) => ({
  default: module.EnterpriseManagePage,
})))

const MailboxManagePage = lazy(() => import('../../pages/system/MailboxManagePage').then((module) => ({
  default: module.MailboxManagePage,
})))

const MailFetchLogPage = lazy(() => import('../../pages/system/MailFetchLogPage').then((module) => ({
  default: module.MailFetchLogPage,
})))

const MailSendLogPage = lazy(() => import('../../pages/system/MailSendLogPage').then((module) => ({
  default: module.MailSendLogPage,
})))

const TicketNumberRulePage = lazy(() => import('../../pages/system/TicketNumberRulePage').then((module) => ({
  default: module.TicketNumberRulePage,
})))

const TicketRuleConfirmModal = lazy(() => import('../../pages/system/TicketRuleConfirmModal').then((module) => ({
  default: module.TicketRuleConfirmModal,
})))

const NotificationTemplatePage = lazy(() => import('../../pages/system/NotificationTemplatePage').then((module) => ({
  default: module.NotificationTemplatePage,
})))

const AssignmentRulePage = lazy(() => import('../../pages/sla/AssignmentRulePage').then((module) => ({
  default: module.AssignmentRulePage,
})))

const SlaPolicyPage = lazy(() => import('../../pages/sla/SlaPolicyPage').then((module) => ({
  default: module.SlaPolicyPage,
})))

const WorkCalendarPage = lazy(() => import('../../pages/sla/WorkCalendarPage').then((module) => ({
  default: module.WorkCalendarPage,
})))

const CustomerManagePage = lazy(() => import('../../pages/customer/CustomerManagePage').then((module) => ({
  default: module.CustomerManagePage,
})))

const TicketListPage = lazy(() => import('../../pages/ticket/TicketListPage').then((module) => ({
  default: module.TicketListPage,
})))

const TicketDetailPage = lazy(() => import('../../pages/ticket/TicketDetailPage').then((module) => ({
  default: module.TicketDetailPage,
})))

const TicketOperationModals = lazy(() => import('../../pages/ticket/TicketOperationModals').then((module) => ({
  default: module.TicketOperationModals,
})))

const DashboardPage = lazy(() => import('../../pages/dashboard/DashboardPage').then((module) => ({
  default: module.DashboardPage,
})))

type AppContentRendererProps = {
  activeMenu: string
  model: AppContentModel
}

function PageLoadingState({ label }: { label: string }) {
  return (
    <section className="app-content" aria-label={`${label}加载中`}>
      <div className="user-loading">
        {[0, 1, 2].map((item) => <span key={item} />)}
      </div>
    </section>
  )
}

function PageUnavailable({ activeMenu }: { activeMenu: string }) {
  return (
    <section className="app-content" aria-label="页面不可用">
      <div className="content-empty-state">
        <h1>页面不可用</h1>
        <p>当前菜单「{activeMenu}」暂未配置页面或当前账号没有访问权限。</p>
      </div>
    </section>
  )
}

export function AppContentRenderer({ activeMenu, model: m }: AppContentRendererProps) {
  return (
    <>
      {activeMenu === '工作台' ? (
        <Suspense fallback={<PageLoadingState label="工作台" />}>
          <DashboardPage
            canOpenTicketList={m.canOpenTicketList}
            dashboardError={m.dashboardError}
            dashboardEnterpriseFilter={m.dashboardEnterpriseFilter}
            dashboardEnterpriseOptions={m.dashboardEnterpriseOptions}
            dashboardLoading={m.dashboardLoading}
            dashboardMailboxFilter={m.dashboardMailboxFilter}
            dashboardMailboxOptions={m.dashboardMailboxOptions}
            dashboardReport={m.dashboardReport}
            dashboardSummary={m.dashboardSummary}
            dashboardTodos={m.dashboardTodos}
            dashboardUpdatedAt={m.dashboardUpdatedAt}
            hasPermission={m.hasPermission}
            onFetchDashboard={() => void m.fetchDashboard()}
            onEnterpriseFilterChange={m.changeDashboardEnterpriseFilter}
            onMailboxFilterChange={m.setDashboardMailboxFilter}
            onNavigateToTickets={(status, slaBreachedOnly, enterpriseId, mailboxId) => {
              if (enterpriseId) m.changeTicketEnterpriseFilter(enterpriseId)
              if (mailboxId) m.changeTicketMailboxFilter(mailboxId)
              m.navigateToTickets(status, slaBreachedOnly)
            }}
            onOpenTicketDetail={(ticketId) => void m.handleOpenDetail(ticketId)}
            onSetActiveMenu={m.setActiveMenu}
          />
        </Suspense>
      ) : activeMenu === '全部工单' ? (
        m.showTicketDetailPage && m.ticketDetail ? (
          <Suspense fallback={<PageLoadingState label="工单详情" />}>
            <TicketDetailPage
              canClaimCurrentTicket={m.canClaimCurrentTicket}
              canOperateCurrentTicket={m.canOperateCurrentTicket}
              claimSending={m.claimSending}
              detail={m.ticketDetail}
              events={m.getVisibleTicketEvents(m.ticketDetail.events)}
              isCurrentTicketTerminal={m.isCurrentTicketTerminal}
              isCurrentTicketUnassigned={m.isCurrentTicketUnassigned}
              msgFilter={m.msgFilter}
              msgSortAsc={m.msgSortAsc}
              onBackToList={m.handleBackToList}
              onClaimTicket={() => void m.handleClaimTicket()}
              onCloseTicket={() => {
                m.setCloseReason('')
                m.setCloseConfirmed(false)
                m.setCloseModalOpen(true)
              }}
              onDeleteAttachment={(attachmentId) => void m.handleDeleteAttachment(attachmentId)}
              onMsgFilterChange={m.setMsgFilter}
              onMsgSortAscChange={m.setMsgSortAsc}
              onOpenAssign={() => {
                m.setAssignUserId(m.ticketDetail.assigneeId)
                m.setAssignReason('')
                m.setAssignNotifyAssignee(true)
                m.setAssignModalOpen(true)
                void m.fetchAgentUsers()
              }}
              onOpenPriority={() => {
                m.setPriorityValue(m.ticketDetail.priority)
                m.setPriorityReason('')
                m.setPriorityModalOpen(true)
              }}
              onOpenStatus={() => {
                m.setStatusValue(m.ticketDetail.status === 'PROCESSING' ? 'CANCELLED' : 'PROCESSING')
                m.setStatusReason('')
                m.setStatusModalOpen(true)
              }}
              onRemoveUploadedFile={m.handleRemoveFile}
              onReply={() => void m.handleReply()}
              onReplyUpdate={(html, text) => {
                m.setReplyHtml(html)
                m.setReplyContent(text)
              }}
              onSaveRemark={(remark) => void m.saveTicketRemark(remark)}
              onTabChange={m.setTicketDetailTab}
              onUploadFile={(file) => void m.handleUploadFile(file)}
              replyContent={m.replyContent}
              replyHtml={m.replyHtml}
              replySending={m.replySending}
              remarkDraft={m.remarkDraft}
              setRemarkDraft={m.setRemarkDraft}
              tab={m.ticketDetailTab}
              ticketAttachments={m.ticketAttachments}
              uploadedFiles={m.uploadedFiles}
              uploadingFile={m.uploadingFile}
            />
          </Suspense>
        ) : (
          <Suspense fallback={<PageLoadingState label="全部工单" />}>
            <TicketListPage
              enterpriseFilter={m.ticketEnterpriseFilter}
              enterpriseOptions={m.ticketEnterpriseOptions}
              isAdmin={m.isAdmin}
              keyword={m.ticketKeyword}
              loading={m.ticketsLoading}
              onClearFilters={m.resetTicketFilters}
              onEnterpriseFilterChange={m.changeTicketEnterpriseFilter}
              onKeywordChange={m.changeTicketKeyword}
              onMailboxFilterChange={m.changeTicketMailboxFilter}
              onOpenDetail={(id) => void m.handleOpenDetail(id)}
              onPageChange={(page) => m.setTicketPage(page)}
              onRefresh={m.refreshTickets}
              onSearch={m.searchTickets}
              onSelectSlaBreached={() => m.navigateToTickets('ALL', true)}
              onStatusChange={m.changeTicketStatus}
              page={m.ticketPage}
              pageSize={m.ticketPageSize}
              mailboxFilter={m.ticketMailboxFilter}
              mailboxOptions={m.ticketMailboxOptions}
              slaBreachedOnly={m.ticketSlaBreachedOnly}
              stats={m.ticketStats}
              statusTab={m.ticketStatusTab}
              ticketsData={m.ticketsData}
              ticketsError={m.ticketsError}
            />
          </Suspense>
        )
      ) : activeMenu === '客户管理' ? (
        <Suspense fallback={<PageLoadingState label="客户管理" />}>
          <CustomerManagePage
            canReadCustomers={m.canReadCustomers}
            customerDetail={m.customerDetail}
            customerDetailError={m.customerDetailError}
            customerDetailLoading={m.customerDetailLoading}
            customerEnterpriseFilter={m.customerEnterpriseFilter}
            customerEnterpriseOptions={m.customerEnterpriseOptions}
            customerKeyword={m.customerKeyword}
            customerMailboxFilter={m.customerMailboxFilter}
            customerMailboxOptions={m.customerMailboxOptions}
            customerPage={m.customerPage}
            customerPageSize={m.customerPageSize}
            customerTicketsData={m.customerTicketsData}
            customerTicketsError={m.customerTicketsError}
            customerTicketsLoading={m.customerTicketsLoading}
            customersData={m.customersData}
            customersError={m.customersError}
            customersLoading={m.customersLoading}
            onFetchCustomerDetail={m.fetchCustomerDetail}
            onFetchCustomerTickets={m.fetchCustomerTickets}
            onFetchCustomers={m.fetchCustomers}
            onEnterpriseFilterChange={m.changeCustomerEnterpriseFilter}
            onKeywordChange={m.changeCustomerKeyword}
            onMailboxFilterChange={m.changeCustomerMailboxFilter}
            onOpenTicket={(ticket, customerEmail) => m.openTicketFromCustomer(ticket.id, customerEmail)}
            onPageChange={m.changeCustomerPage}
            onSearchCustomers={m.searchCustomers}
            onSelectCustomer={m.setSelectedCustomerEmail}
            selectedCustomerEmail={m.selectedCustomerEmail}
          />
        </Suspense>
      ) : activeMenu === '分配规则' ? (
        <Suspense fallback={<PageLoadingState label="分配规则" />}>
          <AssignmentRulePage
            actionLoading={m.assignmentActionLoading}
            assigneeOptions={m.assignmentAssigneeOptions}
            assignmentAssignees={m.assignmentAssignees}
            assignmentEnabledFilter={m.assignmentEnabledFilter}
            canCreateAssignmentRules={m.canCreateAssignmentRules}
            canReadAssignmentRules={m.canReadAssignmentRules}
            enterpriseOptions={m.assignmentEnterpriseOptions}
            groupForm={m.assignmentGroupForm}
            groupSaving={m.assignmentGroupSaving}
            groupsData={m.assignmentGroupsData}
            groupsLoading={m.assignmentGroupsLoading}
            confirmAction={m.assignmentConfirmAction}
            form={m.assignmentForm}
            keyword={m.assignmentKeyword}
            matchResult={m.assignmentMatchResult}
            matchTypeFilter={m.assignmentMatchTypeFilter}
            mailboxOptions={m.assignmentMailboxOptions}
            onCancelConfirm={() => m.setAssignmentConfirmAction(null)}
            onEnabledFilterChange={m.setAssignmentEnabledFilter}
            onFetchAssignmentRules={m.fetchAssignmentRules}
            onFetchAssignmentGroups={m.fetchAssignmentGroups}
            onKeywordChange={m.setAssignmentKeyword}
            onMatchTypeFilterChange={m.setAssignmentMatchTypeFilter}
            onMoveRule={m.moveAssignmentRule}
            onOpenCreateRule={m.openCreateAssignmentRule}
            onOpenCreateGroup={m.openCreateAssignmentGroup}
            onRequestDelete={(rule) => m.setAssignmentConfirmAction({ type: 'delete', rule })}
            onResetFilters={m.resetAssignmentFilters}
            onRunTest={m.runAssignmentRuleTest}
            onSaveRule={m.saveAssignmentRule}
            onSaveGroup={m.saveAssignmentGroup}
            onSelectGroup={m.selectAssignmentGroup}
            onSelectRule={m.selectAssignmentRule}
            onSubmitConfirm={m.submitAssignmentConfirm}
            onTestFormChange={(patch: Partial<AssignmentRuleTestForm>) => {
              m.setAssignmentTestForm((form: AssignmentRuleTestForm) => ({ ...form, ...patch }))
            }}
            onToggleRule={m.toggleAssignmentRule}
            onToggleGroup={m.toggleAssignmentGroup}
            onGroupFormChange={m.setAssignmentGroupForm}
            onEnterpriseChange={m.setSelectedAssignmentEnterpriseId}
            onUpdateForm={m.updateAssignmentForm}
            ruleDirty={m.assignmentRuleDirty}
            rulesData={m.assignmentRulesData}
            rulesError={m.assignmentRulesError}
            rulesLoading={m.assignmentRulesLoading}
            saving={m.assignmentSaving}
            selectedEnterpriseId={m.selectedAssignmentEnterpriseId}
            selectedGroupId={m.selectedAssignmentGroupId}
            selectedRule={m.selectedAssignmentRule}
            testForm={m.assignmentTestForm}
            testing={m.assignmentTesting}
          />
        </Suspense>
      ) : activeMenu === 'SLA策略' ? (
        <Suspense fallback={<PageLoadingState label="SLA策略" />}>
          <SlaPolicyPage
            actionLoading={m.slaPolicyActionLoading}
            calendarCount={m.slaCalendarCount}
            calendarOptions={m.slaCalendarOptions}
            canCreateSlaPolicies={m.canCreateSlaPolicies}
            canReadSlaPolicies={m.canReadSlaPolicies}
            confirmAction={m.slaPolicyConfirmAction}
            enabledFilter={m.slaPolicyEnabledFilter}
            enterpriseFilter={m.slaEnterpriseFilter}
            enterpriseOptions={m.slaEnterpriseOptions}
            form={m.slaPolicyForm}
            keyword={m.slaPolicyKeyword}
            onCancelConfirm={() => m.setSlaPolicyConfirmAction(null)}
            onEnabledFilterChange={m.setSlaPolicyEnabledFilter}
            onEnterpriseFilterChange={m.setSlaEnterpriseFilter}
            onFetchSlaPolicies={m.fetchSlaPolicies}
            onKeywordChange={m.setSlaPolicyKeyword}
            onOpenCreatePolicy={m.openCreateSlaPolicy}
            onRequestDelete={(policy) => m.setSlaPolicyConfirmAction({ type: 'delete', policy })}
            onResetFilters={m.resetSlaPolicyFilters}
            onSavePolicy={m.saveSlaPolicy}
            onSelectPolicy={m.selectSlaPolicy}
            onSubmitConfirm={m.submitSlaPolicyConfirm}
            onTogglePolicy={m.toggleSlaPolicy}
            onUpdateForm={m.updateSlaPolicyForm}
            policiesData={m.slaPoliciesData}
            policiesError={m.slaPoliciesError}
            policiesLoading={m.slaPoliciesLoading}
            policyDirty={m.slaPolicyDirty}
            preview={m.slaPreview}
            previewBaseTime={m.slaPreviewBaseTime}
            resolveHoursInvalid={m.slaResolveHoursInvalid}
            saving={m.slaPolicySaving}
            selectedPolicy={m.selectedSlaPolicy}
            selectedWorkCalendar={m.selectedWorkCalendar}
            warningInvalid={m.slaWarningInvalid}
            workCalendars={m.workCalendars}
            workCalendarsLoading={m.workCalendarsLoading}
            workdayLabel={m.workdayLabel}
          />
        </Suspense>
      ) : activeMenu === '工作日历' ? (
        <Suspense fallback={<PageLoadingState label="工作日历" />}>
          <WorkCalendarPage
            actionLoading={m.workCalendarActionLoading}
            calendarPreviewCreatedAtValue={m.calendarPreviewCreatedAtValue}
            calendarPreviewResponseHours={m.calendarPreviewResponseHours}
            calendarPreviewResponseHoursValue={m.calendarPreviewResponseHoursValue}
            calendarPreviewResolveHours={m.calendarPreviewResolveHours}
            calendarPreviewResolveHoursValue={m.calendarPreviewResolveHoursValue}
            calendarSlaExample={m.calendarSlaExample}
            calendarSlaPolicies={m.calendarSlaPolicies}
            canCreateHolidays={m.canCreateHolidays}
            canCreateWorkCalendars={m.canCreateWorkCalendars}
            canDeleteHolidays={m.canDeleteHolidays}
            canDeleteWorkCalendars={m.canDeleteWorkCalendars}
            canImportHolidays={m.canImportHolidays}
            canReadWorkCalendars={m.canReadWorkCalendars}
            canUpdateHolidays={m.canUpdateHolidays}
            confirmAction={m.workCalendarConfirmAction}
            holidayDateValue={m.holidayDateValue}
            holidayDirty={m.holidayDirty}
            holidayForm={m.holidayForm}
            holidayImportYear={m.holidayImportYear}
            holidayImporting={m.holidayImporting}
            holidayKeyword={m.holidayKeyword}
            holidayMonthValue={m.holidayMonthValue}
            holidayRecords={m.holidayRecords}
            holidaySaving={m.holidaySaving}
            holidaysData={m.holidaysData}
            holidaysError={m.holidaysError}
            holidaysLoading={m.holidaysLoading}
            monthCells={m.monthCells}
            onCalendarPreviewCreatedAtChange={m.setCalendarPreviewCreatedAt}
            onCalendarPreviewResponseHoursChange={m.setCalendarPreviewResponseHours}
            onCalendarPreviewResolveHoursChange={m.setCalendarPreviewResolveHours}
            onCancelConfirm={() => m.setWorkCalendarConfirmAction(null)}
            onConfirmDelete={m.submitWorkCalendarConfirm}
            onFetchAll={m.fetchWorkCalendarPageAll}
            onFetchHolidays={m.fetchHolidays}
            onFetchWorkCalendars={m.fetchWorkCalendarsPage}
            onHolidayKeywordChange={m.setHolidayKeyword}
            onHolidayMonthChange={m.setHolidayMonth}
            onImportNationalHolidays={m.importNationalHolidays}
            onOpenCreateHoliday={m.openCreateHoliday}
            onOpenCreateWorkCalendar={m.openCreateWorkCalendar}
            onRequestDeleteCalendar={(calendar) => m.setWorkCalendarConfirmAction({ type: 'delete-calendar', calendar })}
            onRequestDeleteHoliday={(holiday) => m.setWorkCalendarConfirmAction({ type: 'delete-holiday', holiday })}
            onResetCalendarPreview={m.resetCalendarPreview}
            onResetWorkCalendarFilters={m.resetWorkCalendarFilters}
            onSaveHoliday={m.saveHoliday}
            onSaveWorkCalendar={m.saveWorkCalendar}
            onSelectHoliday={m.selectHoliday}
            onSelectWorkCalendar={m.selectWorkCalendar}
            onSetDefaultWorkCalendar={m.setDefaultWorkCalendar}
            onUpdateHolidayForm={m.updateHolidayForm}
            onUpdateWorkCalendarForm={m.updateWorkCalendarForm}
            onWorkCalendarDefaultFilterChange={m.setWorkCalendarDefaultFilter}
            onWorkCalendarKeywordChange={m.setWorkCalendarKeyword}
            selectedCalendar={m.selectedCalendarForPage}
            timeInvalid={m.workCalendarTimeInvalid}
            workCalendarData={m.workCalendarData}
            workCalendarDefaultFilter={m.workCalendarDefaultFilter}
            workCalendarEnterpriseFilter={m.workCalendarEnterpriseFilter}
            workCalendarEnterpriseOptions={m.workCalendarEnterpriseOptions}
            workCalendarDirty={m.workCalendarDirty}
            workCalendarError={m.workCalendarError}
            workCalendarForm={m.workCalendarForm}
            workCalendarKeyword={m.workCalendarKeyword}
            workCalendarSaving={m.workCalendarSaving}
            workCalendarsLoading={m.workCalendarPageLoading}
            workdayLabel={m.workdayLabel}
            onWorkCalendarEnterpriseFilterChange={m.setWorkCalendarEnterpriseFilter}
          />
        </Suspense>
      ) : activeMenu === '角色管理' ? (
        <Suspense fallback={<PageLoadingState label="角色管理" />}>
          <RoleManagePage
            canCreateRoles={m.canCreateRoles}
            canEnableRoles={m.canEnableRoles}
            canReadRoles={m.canReadRoles}
            canUpdateRoles={m.canUpdateRoles}
            canUpdateRolePermissions={m.canUpdateRolePermissions}
            onCreateRole={m.openCreateRole}
            onFetchRoles={() => void m.fetchRoles()}
            onOpenUserManage={() => m.setActiveMenu('用户管理')}
            onRoleEnabledFilterChange={m.setRoleEnabledFilter}
            onRoleFormChange={m.updateRoleForm}
            onRoleKeywordChange={m.setRoleKeyword}
            onSelectRole={m.selectRole}
            onSubmitRoleBase={m.submitRoleBase}
            onSubmitRolePermissions={m.submitRolePermissions}
            onToggleRoleEnabled={m.toggleRoleEnabled}
            onToggleRolePermission={m.toggleRolePermission}
            permissionTree={m.permissionTree}
            permissionTreeLoading={m.permissionTreeLoading}
            roleDraftMode={m.roleDraftMode}
            roleEnabledFilter={m.roleEnabledFilter}
            roleForm={m.roleForm}
            roleKeyword={m.roleKeyword}
            rolePermissionSaving={m.rolePermissionSaving}
            roleSaving={m.roleSaving}
            rolesData={m.rolesData}
            rolesError={m.rolesError}
            rolesLoading={m.rolesLoading}
            selectedRole={m.selectedRole}
            selectedRoleId={m.selectedRoleId}
            selectedRoleReadonly={m.selectedRoleReadonly}
          />
        </Suspense>
      ) : activeMenu === '企业管理' ? (
        <Suspense fallback={<PageLoadingState label="企业管理" />}>
          <EnterpriseManagePage
            actionLoading={m.enterpriseActionLoading}
            canCreate={m.canCreateEnterprises}
            canEnable={m.canEnableEnterprises}
            canRead={m.canReadEnterprises}
            canUpdate={m.canUpdateEnterprises}
            confirmAction={m.enterpriseConfirmAction}
            data={m.enterprisesData}
            enabledFilter={m.enterpriseEnabledFilter}
            error={m.enterprisesError}
            form={m.enterpriseForm}
            formOpen={m.enterpriseFormOpen}
            keyword={m.enterpriseKeyword}
            loading={m.enterprisesLoading}
            page={m.enterprisePage}
            pageSize={m.enterprisePageSize}
            onCloseForm={() => m.setEnterpriseFormOpen(false)}
            onConfirmActionChange={m.setEnterpriseConfirmAction}
            onEnabledFilterChange={m.setEnterpriseEnabledFilter}
            onFetch={() => void m.fetchEnterprises()}
            onFormChange={m.updateEnterpriseForm}
            onKeywordChange={m.setEnterpriseKeyword}
            onOpenCreate={m.openCreateEnterprise}
            onOpenEdit={m.openEditEnterprise}
            onPageChange={m.setEnterprisePage}
            onPageSizeChange={m.setEnterprisePageSize}
            onSave={() => void m.saveEnterprise()}
            onSubmitConfirm={() => void m.submitEnterpriseConfirm()}
            saving={m.enterpriseSaving}
          />
        </Suspense>
      ) : activeMenu === '用户管理' ? (
        <Suspense fallback={<PageLoadingState label="用户管理" />}>
          <UserManagePage
            actionLoading={m.actionLoading}
            canCreateUsers={m.canCreateUsers}
            canEnableUsers={m.canEnableUsers}
            canReadUsers={m.canReadUsers}
            canResetUserPassword={m.canResetUserPassword}
            canUpdateUsers={m.canUpdateUsers}
            confirmAction={m.confirmAction}
            departmentOptions={m.departmentOptions}
            departmentsError={m.departmentsError}
            onCloseConfirm={m.closeConfirm}
            onCloseUserForm={m.closeUserForm}
            onFetchUsers={m.fetchUsers}
            onOpenCreateUser={m.openCreateUser}
            onOpenEditUser={m.openEditUser}
            onOpenEnabledConfirm={m.openEnabledConfirm}
            onOpenResetConfirm={m.openResetConfirm}
            onResetUserFilters={m.resetUserFilters}
            onSubmitConfirmAction={m.submitConfirmAction}
            onSubmitUserForm={m.submitUserForm}
            onUserEnabledFilterChange={m.changeUserEnabledFilter}
            onUserFormChange={m.updateUserForm}
            onUserKeywordChange={m.changeUserKeyword}
            onUserPageChange={m.setUserPage}
            onUserPageSizeChange={m.changeUserPageSize}
            onUserRoleChange={m.changeUserRole}
            onUserRoleFilterChange={m.changeUserRoleFilter}
            roleOptions={m.roleSelectOptions}
            rolesPermissionTotal={m.rolesData?.permissionTotal}
            userEnabledFilter={m.userEnabledFilter}
            userForm={m.userForm}
            userFormError={m.userFormError}
            userFormMode={m.userFormMode}
            userFormOpen={m.userFormOpen}
            userFormSubmitting={m.userFormSubmitting}
            userKeyword={m.userKeyword}
            userPage={m.userPage}
            userPageSize={m.userPageSize}
            userRoleFilter={m.userRoleFilter}
            usersData={m.usersData}
            usersError={m.usersError}
            usersLoading={m.usersLoading}
            userGrantEnterpriseOptions={m.userGrantEnterpriseOptions}
            userGrantForm={m.userGrantForm}
            userGrantLoading={m.userGrantLoading}
            userGrantMailboxOptions={m.userGrantMailboxOptions}
            onToggleUserGrantEnterprise={m.toggleUserGrantEnterprise}
            onToggleUserGrantMailbox={m.toggleUserGrantMailbox}
          />
        </Suspense>
      ) : activeMenu === '邮箱配置' ? (
        <Suspense fallback={<PageLoadingState label="邮箱配置" />}>
          <MailboxManagePage
            activeMailboxStep={m.activeMailboxStep}
            canCreateMailboxes={m.canCreateMailboxes}
            canDeleteMailboxes={m.canDeleteMailboxes}
            canEnableMailboxes={m.canEnableMailboxes}
            canReadMailboxes={m.canReadMailboxes}
            canTestMailboxes={m.canTestMailboxes}
            canUpdateMailboxes={m.canUpdateMailboxes}
            enterpriseOptions={m.enterpriseOptions}
            mailboxActionLoading={m.mailboxActionLoading}
            mailboxAssignees={m.mailboxAssignees}
            mailboxConfirmAction={m.mailboxConfirmAction}
            mailboxDirty={m.mailboxDirty}
            mailboxEnterpriseFilter={m.mailboxEnterpriseFilter}
            mailboxForm={m.mailboxForm}
            mailboxKeyword={m.mailboxKeyword}
            mailboxPage={m.mailboxPage}
            mailboxPageSize={m.mailboxPageSize}
            mailboxSaving={m.mailboxSaving}
            mailboxesData={m.mailboxesData}
            mailboxesError={m.mailboxesError}
            mailboxesLoading={m.mailboxesLoading}
            mailboxStatusFilter={m.mailboxStatusFilter}
            mailboxTesting={m.mailboxTesting}
            mailboxTestResult={m.mailboxTestResult}
            mailboxTemplateOptions={m.mailboxTemplateOptions}
            mailboxSlaOptions={m.mailboxSlaOptions}
            mailboxRuleGroupOptions={m.mailboxRuleGroupOptions}
            onCloseMailboxConfirm={m.closeMailboxConfirm}
            onFetchMailboxes={m.fetchMailboxes}
            onMailboxConfirm={m.openMailboxConfirm}
            onMailboxKeywordChange={m.changeMailboxKeyword}
            onMailboxEnterpriseFilterChange={m.changeMailboxEnterpriseFilter}
            onMailboxPageChange={m.setMailboxPage}
            onMailboxPageSizeChange={m.changeMailboxPageSize}
            onMailboxStatusFilterChange={m.changeMailboxStatusFilter}
            onMoveMailboxStep={m.moveMailboxStep}
            onOpenCreateMailbox={m.openCreateMailbox}
            onResetMailboxFilters={m.resetMailboxFilters}
            onSaveMailbox={() => void m.saveMailbox()}
            onSelectMailbox={m.selectMailbox}
            onSetActiveMailboxStep={m.setActiveMailboxStep}
            onSubmitMailboxConfirm={m.submitMailboxConfirm}
            onTestMailboxConnection={(testType) => void m.testMailboxConnection(testType)}
            onUpdateMailboxForm={m.updateMailboxForm}
          />
        </Suspense>
      ) : activeMenu === '收件记录' ? (
        <Suspense fallback={<PageLoadingState label="收件记录" />}>
          <MailFetchLogPage
            detail={m.fetchLogDetail}
            error={m.fetchLogsError}
            enterpriseFilter={m.fetchLogEnterpriseFilter}
            enterprises={m.fetchLogEnterpriseOptions}
            loading={m.fetchLogsLoading}
            mailboxFilter={m.fetchLogMailboxFilter}
            mailboxes={m.fetchLogMailboxOptions}
            onClearFilters={m.clearFetchLogFilters}
            onDetailChange={m.setFetchLogDetail}
            onEnterpriseFilterChange={m.changeFetchLogEnterpriseFilter}
            onMailboxFilterChange={m.changeFetchLogMailboxFilter}
            onPageChange={m.changeFetchLogPage}
            onQuery={m.queryFetchLogs}
            onRefresh={m.refreshFetchLogs}
            onSuccessFilterChange={m.changeFetchLogSuccessFilter}
            onTimeRangeChange={m.changeFetchLogTimeRange}
            page={m.fetchLogPage}
            pageSize={m.fetchLogPageSize}
            records={m.fetchLogsData}
            startFrom={m.fetchLogStartFrom}
            startTo={m.fetchLogStartTo}
            stats={m.fetchLogStats}
            successFilter={m.fetchLogSuccessFilter}
          />
        </Suspense>
      ) : activeMenu === '发件记录' ? (
        <Suspense fallback={<PageLoadingState label="发件记录" />}>
          <MailSendLogPage
            detail={m.sendLogDetail}
            error={m.sendLogsError}
            enterpriseFilter={m.sendLogEnterpriseFilter}
            enterprises={m.sendLogEnterpriseOptions}
            loading={m.sendLogsLoading}
            mailboxFilter={m.sendLogMailboxFilter}
            mailboxes={m.sendLogMailboxOptions}
            onClearFilters={m.clearSendLogFilters}
            onDetailChange={m.setSendLogDetail}
            onEnterpriseFilterChange={m.changeSendLogEnterpriseFilter}
            onMailboxFilterChange={m.changeSendLogMailboxFilter}
            onPageChange={m.changeSendLogPage}
            onQuery={m.querySendLogs}
            onRefresh={m.refreshSendLogs}
            onStatusFilterChange={m.changeSendLogStatusFilter}
            onTimeRangeChange={m.changeSendLogTimeRange}
            onTypeFilterChange={m.changeSendLogTypeFilter}
            page={m.sendLogPage}
            pageSize={m.sendLogPageSize}
            records={m.sendLogsData}
            startFrom={m.sendLogStartFrom}
            startTo={m.sendLogStartTo}
            stats={m.sendLogStats}
            statusFilter={m.sendLogStatusFilter}
            typeFilter={m.sendLogTypeFilter}
          />
        </Suspense>
      ) : activeMenu === '编号规则' ? (
        <Suspense fallback={<PageLoadingState label="编号规则" />}>
          <TicketNumberRulePage
            activeSystemGroup={m.activeSystemGroup}
            activeSystemGroupConfig={m.activeSystemGroupConfig}
            canReadTicketNumberRule={m.canReadTicketNumberRule}
            canUpdateTicketNumberRule={m.canUpdateTicketNumberRule}
            onActiveSystemGroupChange={(key) => {
              m.setActiveSystemGroup(key)
              m.clearTicketRuleFeedback()
            }}
            onFetchTicketRule={m.fetchTicketRule}
            onPreviewTicketRule={m.previewTicketRule}
            onRequestSave={m.openTicketRuleConfirm}
            onResetTicketRule={m.resetTicketRule}
            onUpdateTicketRuleForm={m.updateTicketRuleForm}
            systemGroups={m.systemGroups}
            ticketRule={m.ticketRule}
            ticketRuleDirty={m.ticketRuleDirty}
            ticketRuleError={m.ticketRuleError}
            ticketRuleForm={m.ticketRuleForm}
            ticketRuleLoading={m.ticketRuleLoading}
            ticketRuleMessage={m.ticketRuleMessage}
            ticketRulePreviewLoading={m.ticketRulePreviewLoading}
            ticketRuleSaving={m.ticketRuleSaving}
          />
        </Suspense>
      ) : activeMenu === '通知模板' ? (
        <Suspense fallback={<PageLoadingState label="通知模板" />}>
          <NotificationTemplatePage
            canReadTemplates={m.canReadTemplates}
            confirmOpen={m.templateConfirmOpen}
            onCancelConfirm={() => m.setTemplateConfirmOpen(false)}
            onClearKeyword={() => m.setTemplateKeyword('')}
            onFetchTemplates={m.fetchTemplates}
            onOpenCreateTemplate={m.openCreateTemplate}
            onPreviewTemplate={m.previewTemplate}
            onRequestSave={() => m.setTemplateConfirmOpen(true)}
            onSaveTemplate={m.saveTemplate}
            onSelectTemplate={m.selectTemplate}
            onTemplateKeywordChange={m.setTemplateKeyword}
            onUpdateTemplateForm={m.updateTemplateForm}
            selectedTemplateId={m.selectedTemplateId}
            templateDirty={m.templateDirty}
            templateForm={m.templateForm}
            templateKeyword={m.templateKeyword}
            templatePreview={m.templatePreview}
            templatePreviewLoading={m.templatePreviewLoading}
            templateSaving={m.templateSaving}
            templateTypeFilter={m.templateTypeFilter}
            onTemplateTypeFilterChange={m.setTemplateTypeFilter}
            templatesData={m.templatesData}
            templatesError={m.templatesError}
            templatesLoading={m.templatesLoading}
          />
        </Suspense>
      ) : activeMenu === '组织管理' ? (
        <Suspense fallback={<PageLoadingState label="组织管理" />}>
          <DepartmentManagePage
            canCreateDepartments={m.canCreateDepartments}
            canEnableDepartments={m.canEnableDepartments}
            canReadDepartments={m.canReadDepartments}
            canUpdateDepartments={m.canUpdateDepartments}
            onAuthExpired={m.handleAuthExpired}
            roleOptions={m.roleSelectOptions}
          />
        </Suspense>
      ) : (
        <PageUnavailable activeMenu={activeMenu} />
      )}

      {m.ticketRuleConfirmOpen && (
        <Suspense fallback={null}>
          <TicketRuleConfirmModal
            onCancel={m.closeTicketRuleConfirm}
            onConfirm={() => void m.saveTicketRule()}
            rule={m.ticketRule}
            saving={m.ticketRuleSaving}
            ticketRuleForm={m.ticketRuleForm}
          />
        </Suspense>
      )}

      <Suspense fallback={null}>
        <TicketOperationModals
          assignModalOpen={m.assignModalOpen}
          assignNotifyAssignee={m.assignNotifyAssignee}
          assignReason={m.assignReason}
          assignSending={m.assignSending}
          assignUserId={m.assignUserId}
          assignUsers={m.assignUsers}
          canOperateCurrentTicket={m.canOperateCurrentTicket}
          closeConfirmed={m.closeConfirmed}
          closeModalOpen={m.closeModalOpen}
          closeReason={m.closeReason}
          closeSending={m.closeSending}
          onAssignNotifyChange={m.setAssignNotifyAssignee}
          onAssignReasonChange={m.setAssignReason}
          onAssignUserChange={m.setAssignUserId}
          onCancelAssign={() => {
            m.setAssignModalOpen(false)
            m.setAssignUserId(null)
            m.setAssignReason('')
            m.setAssignNotifyAssignee(true)
          }}
          onCancelClose={() => {
            m.setCloseModalOpen(false)
            m.setCloseReason('')
            m.setCloseConfirmed(false)
          }}
          onCancelPriority={() => {
            m.setPriorityModalOpen(false)
            m.setPriorityReason('')
          }}
          onCancelStatus={() => {
            m.setStatusModalOpen(false)
            m.setStatusReason('')
          }}
          onCloseConfirmedChange={m.setCloseConfirmed}
          onCloseReasonChange={m.setCloseReason}
          onPriorityReasonChange={m.setPriorityReason}
          onPriorityValueChange={m.setPriorityValue}
          onStatusReasonChange={m.setStatusReason}
          onStatusValueChange={m.setStatusValue}
          onSubmitAssign={() => void m.handleAssign()}
          onSubmitClose={() => void m.handleClose()}
          onSubmitPriority={() => void m.handlePriority()}
          onSubmitStatus={() => void m.handleStatusChange()}
          priorityModalOpen={m.priorityModalOpen}
          priorityReason={m.priorityReason}
          prioritySending={m.prioritySending}
          priorityValue={m.priorityValue}
          statusModalOpen={m.statusModalOpen}
          statusReason={m.statusReason}
          statusSending={m.statusSending}
          statusValue={m.statusValue}
          ticketDetail={m.ticketDetail}
        />
      </Suspense>
    </>
  )
}
