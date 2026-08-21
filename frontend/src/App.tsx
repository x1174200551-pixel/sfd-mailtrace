import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import './App.css'
import { AppContentRenderer } from './components/app/AppContentRenderer'
import type { AppContentModel } from './components/app/AppContentRenderer'
import { AppShell } from './components/layout/AppShell'
import type { AppShellSearchResult } from './components/layout/AppShell'
import { useAssignmentRuleManagement } from './hooks/useAssignmentRuleManagement'
import { useAuthSession } from './hooks/useAuthSession'
import { useMailFetchLogs } from './hooks/useMailFetchLogs'
import { useMailSendLogs } from './hooks/useMailSendLogs'
import { useMailboxManagement } from './hooks/useMailboxManagement'
import { useMailboxReferenceData } from './hooks/useMailboxReferenceData'
import { useDashboardManagement } from './hooks/useDashboardManagement'
import { usePermission } from './hooks/usePermission'
import { useRoleManagement } from './hooks/useRoleManagement'
import { useNotificationTemplateManagement } from './hooks/useNotificationTemplateManagement'
import { useSlaPolicyManagement } from './hooks/useSlaPolicyManagement'
import { useTicketManagement } from './hooks/useTicketManagement'
import { useTicketOperations } from './hooks/useTicketOperations'
import { useUserDepartmentOptions } from './hooks/useUserDepartmentOptions'
import { useUserManagement } from './hooks/useUserManagement'
import { useWorkCalendarManagement } from './hooks/useWorkCalendarManagement'
import { builtInRoleOptions } from './constants/roles'
import { systemGroups } from './constants/system-config'
import { useCustomerManagement } from './hooks/useCustomerManagement'
import { useTicketNumberRuleManagement } from './hooks/useTicketNumberRuleManagement'
import { LoginPage } from './pages/auth/LoginPage'
import type { LoginModalState } from './pages/auth/LoginPage'
import { CustomerTicketLookupPage } from './pages/customer/CustomerTicketLookupPage'
import type { SystemGroupKey } from './types/system-config'
import { getVisibleTicketEvents, isTerminalTicket } from './utils/ticket-events'
import { workdayLabel } from './utils/work-calendar'

function AdminApp() {
  const {
    account,
    accountError,
    changeAccount,
    changePassword,
    checkingSession,
    formError,
    handleAuthExpired,
    handleLogout,
    handleSubmit,
    password,
    passwordError,
    rememberMe,
    setRememberMe,
    setShowPassword,
    showPassword,
    submitting,
    token,
    user,
  } = useAuthSession()
  const [modal, setModal] = useState<LoginModalState>(null)
  const [activeMenu, setActiveMenu] = useState('工作台')
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [activeSystemGroup, setActiveSystemGroup] = useState<SystemGroupKey>('ticket')
  const searchInputRef = useRef<HTMLInputElement>(null)
  const { canAccessPage, firstVisibleMenuTitle, hasPermission, isAdmin, isAgent, visibleMenuGroups } = usePermission(user)
  const canOpenTicketList = hasPermission('menu:tickets') && hasPermission('ticket:read')
  const canReadDashboard = hasPermission('dashboard:read')
  const canReadTickets = hasPermission('ticket:read')
  const canReadCustomers = hasPermission('customer:read')
  const canReadUsers = hasPermission('user:read')
  const canCreateUsers = hasPermission('user:create')
  const canUpdateUsers = hasPermission('user:update')
  const canEnableUsers = hasPermission('user:enable')
  const canResetUserPassword = hasPermission('user:reset_password')
  const canReadRoles = hasPermission('role:read')
  const canCreateRoles = hasPermission('role:create')
  const canUpdateRoles = hasPermission('role:update')
  const canEnableRoles = hasPermission('role:enable')
  const canUpdateRolePermissions = hasPermission('role:permission_update')
  const canReadMailboxes = hasPermission('mailbox:read') || hasPermission('menu:mailboxes')
  const canCreateMailboxes = hasPermission('mailbox:create')
  const canUpdateMailboxes = hasPermission('mailbox:update')
  const canEnableMailboxes = hasPermission('mailbox:enable')
  const canDeleteMailboxes = hasPermission('mailbox:delete')
  const canTestMailboxes = hasPermission('mailbox:test_connection')
  const canReadFetchLogs = hasPermission('mail_fetch_log:read')
  const canReadSendLogs = hasPermission('mail_send_log:read')
  const canReadAssignmentRules = hasPermission('assignment_rule:read')
  const canCreateAssignmentRules = hasPermission('assignment_rule:create')
  const canReadSlaPolicies = hasPermission('sla_policy:read')
  const canCreateSlaPolicies = hasPermission('sla_policy:create')
  const canReadWorkCalendars = hasPermission('work_calendar:read')
  const canCreateWorkCalendars = hasPermission('work_calendar:create')
  const canDeleteWorkCalendars = hasPermission('work_calendar:delete')
  const canReadHolidays = hasPermission('holiday:read')
  const canCreateHolidays = hasPermission('holiday:create')
  const canUpdateHolidays = hasPermission('holiday:update')
  const canDeleteHolidays = hasPermission('holiday:delete')
  const canImportHolidays = hasPermission('holiday:import')
  const canReadDepartments = hasPermission('department:read')
  const canCreateDepartments = hasPermission('department:create')
  const canUpdateDepartments = hasPermission('department:update')
  const canEnableDepartments = hasPermission('department:enable')
  const canReadTicketNumberRule = hasPermission('ticket_number_rule:read')
  const canUpdateTicketNumberRule = hasPermission('ticket_number_rule:update')
  const canReadTemplates = hasPermission('notification_template:read')
  const {
    changeTicketKeyword,
    changeTicketStatus,
    fetchTicketStats,
    fetchTickets,
    handleBackToList,
    handleOpenDetail,
    msgFilter,
    msgSortAsc,
    navigateToTickets,
    openTicketFromCustomer,
    refreshTickets,
    reloadTicketDetail,
    remarkDraft,
    resetTicketFilters,
    saveTicketRemark,
    searchTickets,
    searchTicketsByKeyword,
    setMsgFilter,
    setMsgSortAsc,
    setRemarkDraft,
    setTicketAttachments,
    setTicketDetailTab,
    setTicketPage,
    showTicketDetailPage,
    ticketAttachments,
    ticketDetail,
    ticketDetailTab,
    ticketKeyword,
    ticketPage,
    ticketPageSize,
    ticketsData,
    ticketsError,
    ticketsLoading,
    ticketSlaBreachedOnly,
    ticketStats,
    ticketStatusTab,
  } = useTicketManagement({
    activeMenu,
    canOpenTicketList,
    canReadTickets,
    handleAuthExpired,
    onActiveMenuChange: setActiveMenu,
    token,
  })
  const isCurrentTicketUnassigned = ticketDetail?.assigneeId == null
  const isCurrentTicketTerminal = ticketDetail ? isTerminalTicket(ticketDetail.status) : false
  const canOperateCurrentTicket = !!ticketDetail && !isCurrentTicketTerminal
    && (isAdmin || ticketDetail.assigneeId === user?.id)
  const canClaimCurrentTicket = !!ticketDetail && !isCurrentTicketTerminal
    && isCurrentTicketUnassigned && (isAdmin || isAgent)
  const activeSystemGroupConfig = systemGroups.find((group) => group.key === activeSystemGroup) || systemGroups[0]
  const {
    defaultDepartmentId,
    departmentOptions,
    departmentsError,
  } = useUserDepartmentOptions({
    activeMenu,
    canReadUsers,
    handleAuthExpired,
    token,
  })
  const visibleMenuItems = useMemo(
    () => visibleMenuGroups.flatMap((group) => group.items.map((item) => ({
      groupTitle: group.title,
      title: item.title,
    }))),
    [visibleMenuGroups],
  )
  const globalSearchMenuResults = useMemo<AppShellSearchResult[]>(() => {
    const keyword = searchKeyword.trim()
    if (!keyword) return []
    return visibleMenuItems
      .filter((item) => item.title.includes(keyword) || item.groupTitle.includes(keyword))
      .slice(0, 6)
  }, [searchKeyword, visibleMenuItems])

  useEffect(() => {
    if (!user || !firstVisibleMenuTitle) return
    if (canAccessPage(activeMenu)) return
    setActiveMenu(firstVisibleMenuTitle)
  }, [activeMenu, canAccessPage, firstVisibleMenuTitle, user])
  const handleMenuChange = useCallback((menu: string) => {
    setActiveMenu(menu)
    if (menu !== '全部工单') {
      handleBackToList()
    }
  }, [handleBackToList])

  const {
    fetchRoles,
    openCreateRole,
    permissionTree,
    permissionTreeLoading,
    roleDraftMode,
    roleEnabledFilter,
    roleForm,
    roleKeyword,
    rolePermissionSaving,
    roleSaving,
    rolesData,
    rolesError,
    rolesLoading,
    selectedRole,
    selectedRoleId,
    selectedRoleReadonly,
    selectRole,
    setRoleEnabledFilter,
    setRoleKeyword,
    submitRoleBase,
    submitRolePermissions,
    toggleRoleEnabled,
    toggleRolePermission,
    updateRoleForm,
    updateRoleScope,
  } = useRoleManagement({
    activeMenu,
    canEnableRoles,
    canReadRoles,
    handleAuthExpired,
    token,
  })
  const roleSelectOptions = useMemo(
    () => {
      const options = rolesData?.records.map((role) => ({ label: role.roleName, value: role.roleCode })) ?? []
      return options.length > 0 ? options : builtInRoleOptions
    },
    [rolesData],
  )
  const {
    actionLoading,
    changeUserEnabledFilter,
    changeUserKeyword,
    changeUserPageSize,
    changeUserRole,
    changeUserRoleFilter,
    closeConfirm,
    closeUserForm,
    confirmAction,
    fetchUsers,
    openCreateUser,
    openEditUser,
    openEnabledConfirm,
    openResetConfirm,
    resetUserFilters,
    setUserPage,
    submitConfirmAction,
    submitUserForm,
    updateUserForm,
    userEnabledFilter,
    userForm,
    userFormError,
    userFormMode,
    userFormOpen,
    userFormSubmitting,
    userKeyword,
    userPage,
    userPageSize,
    userRoleFilter,
    usersData,
    usersError,
    usersLoading,
  } = useUserManagement({
    activeMenu,
    canReadUsers,
    defaultDepartmentId,
    handleAuthExpired,
    token,
  })
  const {
    activeMailboxStep,
    changeMailboxKeyword,
    changeMailboxPageSize,
    changeMailboxStatusFilter,
    closeMailboxConfirm,
    fetchMailboxes,
    mailboxActionLoading,
    mailboxConfirmAction,
    mailboxDirty,
    mailboxForm,
    mailboxKeyword,
    mailboxPage,
    mailboxPageSize,
    mailboxSaving,
    mailboxesData,
    mailboxesError,
    mailboxesLoading,
    mailboxStatusFilter,
    mailboxTesting,
    mailboxTestResult,
    moveMailboxStep,
    openCreateMailbox,
    openMailboxConfirm,
    resetMailboxFilters,
    saveMailbox,
    selectMailbox,
    setActiveMailboxStep,
    setMailboxPage,
    submitMailboxConfirm,
    testMailboxConnection,
    updateMailboxForm,
  } = useMailboxManagement({
    activeMenu,
    canCreateMailboxes,
    canDeleteMailboxes,
    canEnableMailboxes,
    canReadMailboxes,
    canTestMailboxes,
    canUpdateMailboxes,
    handleAuthExpired,
    token,
  })
  const {
    mailboxAssignees,
    mailboxes,
  } = useMailboxReferenceData({
    activeMenu,
    canReadUsers,
    handleAuthExpired,
    token,
  })
  const {
    changeFetchLogMailboxFilter,
    changeFetchLogPage,
    changeFetchLogSuccessFilter,
    changeFetchLogTimeRange,
    clearFetchLogFilters,
    fetchLogDetail,
    fetchLogMailboxFilter,
    fetchLogPage,
    fetchLogPageSize,
    fetchLogStartFrom,
    fetchLogStartTo,
    fetchLogStats,
    fetchLogSuccessFilter,
    fetchLogsData,
    fetchLogsError,
    fetchLogsLoading,
    queryFetchLogs,
    refreshFetchLogs,
    setFetchLogDetail,
  } = useMailFetchLogs({
    activeMenu,
    canReadFetchLogs,
    handleAuthExpired,
    token,
  })
  const {
    changeSendLogMailboxFilter,
    changeSendLogPage,
    changeSendLogStatusFilter,
    changeSendLogTimeRange,
    changeSendLogTypeFilter,
    clearSendLogFilters,
    querySendLogs,
    refreshSendLogs,
    sendLogDetail,
    sendLogMailboxFilter,
    sendLogPage,
    sendLogPageSize,
    sendLogStartFrom,
    sendLogStartTo,
    sendLogStats,
    sendLogStatusFilter,
    sendLogTypeFilter,
    sendLogsData,
    sendLogsError,
    sendLogsLoading,
    sendPendingCount,
    setSendLogDetail,
  } = useMailSendLogs({
    activeMenu,
    canReadSendLogs,
    handleAuthExpired,
    token,
  })
  const {
    assignmentActionLoading,
    assignmentAssigneeOptions,
    assignmentAssignees,
    assignmentConfirmAction,
    assignmentEnabledFilter,
    assignmentForm,
    assignmentKeyword,
    assignmentMailboxOptions,
    assignmentMatchResult,
    assignmentMatchTypeFilter,
    assignmentRuleDirty,
    assignmentRulesData,
    assignmentRulesError,
    assignmentRulesLoading,
    assignmentSaving,
    assignmentTestForm,
    assignmentTesting,
    fetchAssignmentRules,
    moveAssignmentRule,
    openCreateAssignmentRule,
    resetAssignmentFilters,
    runAssignmentRuleTest,
    saveAssignmentRule,
    selectAssignmentRule,
    selectedAssignmentRule,
    setAssignmentConfirmAction,
    setAssignmentEnabledFilter,
    setAssignmentKeyword,
    setAssignmentMatchTypeFilter,
    setAssignmentTestForm,
    submitAssignmentConfirm,
    toggleAssignmentRule,
    updateAssignmentForm,
  } = useAssignmentRuleManagement({
    activeMenu,
    canReadAssignmentRules,
    canReadUsers,
    handleAuthExpired,
    mailboxes,
    token,
  })
  const {
    fetchTemplates,
    openCreateTemplate,
    previewTemplate,
    saveTemplate,
    selectTemplate,
    selectedTemplateId,
    setTemplateConfirmOpen,
    setTemplateKeyword,
    templateConfirmOpen,
    templateDirty,
    templateForm,
    templateKeyword,
    templatePreview,
    templatePreviewLoading,
    templateSaving,
    templatesData,
    templatesError,
    templatesLoading,
    updateTemplateForm,
  } = useNotificationTemplateManagement({
    activeMenu,
    canReadTemplates,
    handleAuthExpired,
    token,
  })
  const {
    changeCustomerKeyword,
    changeCustomerPage,
    customerDetail,
    customerDetailError,
    customerDetailLoading,
    customerKeyword,
    customerPage,
    customerPageSize,
    customerTicketsData,
    customerTicketsError,
    customerTicketsLoading,
    customersData,
    customersError,
    customersLoading,
    fetchCustomerDetail,
    fetchCustomerTickets,
    fetchCustomers,
    searchCustomers,
    selectedCustomerEmail,
    setSelectedCustomerEmail,
  } = useCustomerManagement({
    activeMenu,
    canReadCustomers,
    handleAuthExpired,
    token,
  })
  const {
    dashboardError,
    dashboardLoading,
    dashboardReport,
    dashboardSummary,
    dashboardTodos,
    dashboardUpdatedAt,
    fetchDashboard,
  } = useDashboardManagement({
    activeMenu,
    canReadDashboard,
    handleAuthExpired,
    token,
  })
  const {
    clearTicketRuleFeedback,
    closeTicketRuleConfirm,
    fetchTicketRule,
    openTicketRuleConfirm,
    previewTicketRule,
    resetTicketRule,
    saveTicketRule,
    ticketRule,
    ticketRuleConfirmOpen,
    ticketRuleDirty,
    ticketRuleError,
    ticketRuleForm,
    ticketRuleLoading,
    ticketRuleMessage,
    ticketRulePreviewLoading,
    ticketRuleSaving,
    updateTicketRuleForm,
  } = useTicketNumberRuleManagement({
    activeMenu,
    canReadTicketNumberRule,
    handleAuthExpired,
    token,
  })
  const {
    fetchSlaPolicies,
    openCreateSlaPolicy,
    resetSlaPolicyFilters,
    saveSlaPolicy,
    selectSlaPolicy,
    selectedSlaPolicy,
    selectedWorkCalendar,
    setDefaultSlaPolicy,
    setSlaPolicyConfirmAction,
    setSlaPolicyDefaultFilter,
    setSlaPolicyEnabledFilter,
    setSlaPolicyKeyword,
    slaCalendarCount,
    slaCalendarOptions,
    slaPoliciesData,
    slaPoliciesError,
    slaPoliciesLoading,
    slaPolicyActionLoading,
    slaPolicyConfirmAction,
    slaPolicyDefaultFilter,
    slaPolicyDirty,
    slaPolicyEnabledFilter,
    slaPolicyForm,
    slaPolicyKeyword,
    slaPolicySaving,
    slaPreview,
    slaPreviewBaseTime,
    slaResolveHoursInvalid,
    slaWarningInvalid,
    submitSlaPolicyConfirm,
    toggleSlaPolicy,
    updateSlaPolicyForm,
    workCalendars,
    workCalendarsLoading,
  } = useSlaPolicyManagement({
    activeMenu,
    canReadSlaPolicies,
    canReadWorkCalendars,
    handleAuthExpired,
    token,
  })
  const {
    calendarPreviewCreatedAtValue,
    calendarPreviewResponseHours,
    calendarPreviewResponseHoursValue,
    calendarPreviewResolveHours,
    calendarPreviewResolveHoursValue,
    calendarSlaExample,
    calendarSlaPolicies,
    fetchHolidays,
    fetchWorkCalendarPageAll,
    fetchWorkCalendarsPage,
    holidayDateValue,
    holidayDirty,
    holidayForm,
    holidayImportYear,
    holidayImporting,
    holidayKeyword,
    holidayMonthValue,
    holidayRecords,
    holidaySaving,
    holidaysData,
    holidaysError,
    holidaysLoading,
    importNationalHolidays,
    monthCells,
    openCreateHoliday,
    openCreateWorkCalendar,
    resetCalendarPreview,
    resetWorkCalendarFilters,
    saveHoliday,
    saveWorkCalendar,
    selectHoliday,
    selectedCalendarForPage,
    selectWorkCalendar,
    setCalendarPreviewCreatedAt,
    setCalendarPreviewResponseHours,
    setCalendarPreviewResolveHours,
    setDefaultWorkCalendar,
    setHolidayKeyword,
    setHolidayMonth,
    setWorkCalendarConfirmAction,
    setWorkCalendarDefaultFilter,
    setWorkCalendarKeyword,
    submitWorkCalendarConfirm,
    updateHolidayForm,
    updateWorkCalendarForm,
    workCalendarActionLoading,
    workCalendarConfirmAction,
    workCalendarData,
    workCalendarDefaultFilter,
    workCalendarDirty,
    workCalendarError,
    workCalendarForm,
    workCalendarKeyword,
    workCalendarSaving,
    workCalendarsLoading: workCalendarPageLoading,
    workCalendarTimeInvalid,
  } = useWorkCalendarManagement({
    activeMenu,
    canReadHolidays,
    canReadSlaPolicies,
    canReadWorkCalendars,
    handleAuthExpired,
    token,
  })
  useEffect(() => {
    function focusSearch(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        searchInputRef.current?.focus()
      }
    }

    document.addEventListener('keydown', focusSearch)
    return () => document.removeEventListener('keydown', focusSearch)
  }, [])

  const handleGlobalSearch = useCallback(() => {
    const keyword = searchKeyword.trim()
    if (!keyword) return

    const menuMatch = visibleMenuItems.find((item) => item.title === keyword)
      || visibleMenuItems.find((item) => item.title.includes(keyword) || item.groupTitle.includes(keyword))
    if (menuMatch) {
      setActiveMenu(menuMatch.title)
      setSearchKeyword('')
      setProfileOpen(false)
      setNotificationsOpen(false)
      return
    }

    void searchTicketsByKeyword(keyword)
  }, [searchKeyword, searchTicketsByKeyword, visibleMenuItems])

  const handleGlobalSearchResultSelect = useCallback((title: string) => {
    setActiveMenu(title)
    setSearchKeyword('')
    setProfileOpen(false)
    setNotificationsOpen(false)
  }, [])

  const {
    assignModalOpen,
    assignNotifyAssignee,
    assignReason,
    assignSending,
    assignUserId,
    assignUsers,
    claimSending,
    closeConfirmed,
    closeModalOpen,
    closeReason,
    closeSending,
    fetchAgentUsers,
    handleAssign,
    handleClaimTicket,
    handleClose,
    handleDeleteAttachment,
    handlePriority,
    handleRemoveFile,
    handleReply,
    handleStatusChange,
    handleUploadFile,
    priorityModalOpen,
    priorityReason,
    prioritySending,
    priorityValue,
    replyContent,
    replyHtml,
    replySending,
    setAssignModalOpen,
    setAssignNotifyAssignee,
    setAssignReason,
    setAssignUserId,
    setCloseConfirmed,
    setCloseModalOpen,
    setCloseReason,
    setPriorityModalOpen,
    setPriorityReason,
    setPriorityValue,
    setReplyContent,
    setReplyHtml,
    setStatusModalOpen,
    setStatusReason,
    setStatusValue,
    statusModalOpen,
    statusReason,
    statusSending,
    statusValue,
    uploadedFiles,
    uploadingFile,
  } = useTicketOperations({
    canOperateCurrentTicket,
    fetchTicketStats,
    fetchTickets,
    handleAuthExpired,
    reloadTicketDetail,
    setTicketAttachments,
    ticketDetail,
    token,
  })

  const contentModel = {
    actionLoading,
    activeMailboxStep,
    activeSystemGroup,
    activeSystemGroupConfig,
    assignModalOpen,
    assignNotifyAssignee,
    assignReason,
    assignSending,
    assignUserId,
    assignUsers,
    assignmentActionLoading,
    assignmentAssigneeOptions,
    assignmentAssignees,
    assignmentConfirmAction,
    assignmentEnabledFilter,
    assignmentForm,
    assignmentKeyword,
    assignmentMailboxOptions,
    assignmentMatchResult,
    assignmentMatchTypeFilter,
    assignmentRuleDirty,
    assignmentRulesData,
    assignmentRulesError,
    assignmentRulesLoading,
    assignmentSaving,
    assignmentTestForm,
    assignmentTesting,
    calendarPreviewCreatedAtValue,
    calendarPreviewResolveHours,
    calendarPreviewResolveHoursValue,
    calendarPreviewResponseHours,
    calendarPreviewResponseHoursValue,
    calendarSlaExample,
    calendarSlaPolicies,
    canClaimCurrentTicket,
    canCreateAssignmentRules,
    canCreateDepartments,
    canCreateHolidays,
    canCreateMailboxes,
    canCreateRoles,
    canCreateSlaPolicies,
    canCreateUsers,
    canCreateWorkCalendars,
    canDeleteHolidays,
    canDeleteMailboxes,
    canDeleteWorkCalendars,
    canEnableDepartments,
    canEnableMailboxes,
    canEnableRoles,
    canEnableUsers,
    canImportHolidays,
    canOpenTicketList,
    canOperateCurrentTicket,
    canReadAssignmentRules,
    canReadCustomers,
    canReadDepartments,
    canReadMailboxes,
    canReadRoles,
    canReadSlaPolicies,
    canReadTemplates,
    canReadTicketNumberRule,
    canReadUsers,
    canReadWorkCalendars,
    canResetUserPassword,
    canTestMailboxes,
    canUpdateDepartments,
    canUpdateHolidays,
    canUpdateMailboxes,
    canUpdateRolePermissions,
    canUpdateRoles,
    canUpdateTicketNumberRule,
    canUpdateUsers,
    changeCustomerKeyword,
    changeCustomerPage,
    changeFetchLogMailboxFilter,
    changeFetchLogPage,
    changeFetchLogSuccessFilter,
    changeFetchLogTimeRange,
    changeMailboxKeyword,
    changeMailboxPageSize,
    changeMailboxStatusFilter,
    changeSendLogMailboxFilter,
    changeSendLogPage,
    changeSendLogStatusFilter,
    changeSendLogTimeRange,
    changeSendLogTypeFilter,
    changeTicketKeyword,
    changeTicketStatus,
    changeUserEnabledFilter,
    changeUserKeyword,
    changeUserPageSize,
    changeUserRole,
    changeUserRoleFilter,
    claimSending,
    clearFetchLogFilters,
    clearSendLogFilters,
    clearTicketRuleFeedback,
    closeConfirm,
    closeConfirmed,
    closeMailboxConfirm,
    closeModalOpen,
    closeReason,
    closeSending,
    closeTicketRuleConfirm,
    closeUserForm,
    confirmAction,
    customerDetail,
    customerDetailError,
    customerDetailLoading,
    customerKeyword,
    customerPage,
    customerPageSize,
    customerTicketsData,
    customerTicketsError,
    customerTicketsLoading,
    customersData,
    customersError,
    customersLoading,
    dashboardError,
    dashboardLoading,
    dashboardReport,
    dashboardSummary,
    dashboardTodos,
    dashboardUpdatedAt,
    departmentOptions,
    departmentsError,
    fetchAgentUsers,
    fetchAssignmentRules,
    fetchCustomerDetail,
    fetchCustomerTickets,
    fetchCustomers,
    fetchDashboard,
    fetchHolidays,
    fetchLogDetail,
    fetchLogMailboxFilter,
    fetchLogPage,
    fetchLogPageSize,
    fetchLogStartFrom,
    fetchLogStartTo,
    fetchLogStats,
    fetchLogSuccessFilter,
    fetchLogsData,
    fetchLogsError,
    fetchLogsLoading,
    fetchMailboxes,
    fetchRoles,
    fetchSlaPolicies,
    fetchTemplates,
    fetchTicketRule,
    fetchUsers,
    fetchWorkCalendarPageAll,
    fetchWorkCalendarsPage,
    getVisibleTicketEvents,
    handleAssign,
    handleAuthExpired,
    handleBackToList,
    handleClaimTicket,
    handleClose,
    handleDeleteAttachment,
    handleOpenDetail,
    handlePriority,
    handleRemoveFile,
    handleReply,
    handleStatusChange,
    handleUploadFile,
    hasPermission,
    holidayDateValue,
    holidayDirty,
    holidayForm,
    holidayImportYear,
    holidayImporting,
    holidayKeyword,
    holidayMonthValue,
    holidayRecords,
    holidaySaving,
    holidaysData,
    holidaysError,
    holidaysLoading,
    importNationalHolidays,
    isAdmin,
    isCurrentTicketTerminal,
    isCurrentTicketUnassigned,
    mailboxActionLoading,
    mailboxAssignees,
    mailboxConfirmAction,
    mailboxDirty,
    mailboxForm,
    mailboxKeyword,
    mailboxPage,
    mailboxPageSize,
    mailboxSaving,
    mailboxStatusFilter,
    mailboxTestResult,
    mailboxTesting,
    mailboxes,
    mailboxesData,
    mailboxesError,
    mailboxesLoading,
    monthCells,
    moveAssignmentRule,
    moveMailboxStep,
    msgFilter,
    msgSortAsc,
    navigateToTickets,
    openCreateAssignmentRule,
    openCreateHoliday,
    openCreateMailbox,
    openCreateRole,
    openCreateSlaPolicy,
    openCreateTemplate,
    openCreateUser,
    openCreateWorkCalendar,
    openEditUser,
    openEnabledConfirm,
    openMailboxConfirm,
    openResetConfirm,
    openTicketFromCustomer,
    openTicketRuleConfirm,
    permissionTree,
    permissionTreeLoading,
    previewTemplate,
    previewTicketRule,
    priorityModalOpen,
    priorityReason,
    prioritySending,
    priorityValue,
    queryFetchLogs,
    querySendLogs,
    refreshFetchLogs,
    refreshSendLogs,
    refreshTickets,
    remarkDraft,
    replyContent,
    replyHtml,
    replySending,
    resetAssignmentFilters,
    resetCalendarPreview,
    resetMailboxFilters,
    resetSlaPolicyFilters,
    resetTicketFilters,
    resetTicketRule,
    resetUserFilters,
    resetWorkCalendarFilters,
    roleDraftMode,
    roleEnabledFilter,
    roleForm,
    roleKeyword,
    rolePermissionSaving,
    roleSaving,
    roleSelectOptions,
    rolesData,
    rolesError,
    rolesLoading,
    runAssignmentRuleTest,
    saveAssignmentRule,
    saveHoliday,
    saveMailbox,
    saveSlaPolicy,
    saveTemplate,
    saveTicketRemark,
    saveTicketRule,
    saveWorkCalendar,
    searchCustomers,
    searchTickets,
    selectAssignmentRule,
    selectHoliday,
    selectMailbox,
    selectRole,
    selectSlaPolicy,
    selectTemplate,
    selectWorkCalendar,
    selectedAssignmentRule,
    selectedCalendarForPage,
    selectedCustomerEmail,
    selectedRole,
    selectedRoleId,
    selectedRoleReadonly,
    selectedSlaPolicy,
    selectedTemplateId,
    selectedWorkCalendar,
    sendLogDetail,
    sendLogMailboxFilter,
    sendLogPage,
    sendLogPageSize,
    sendLogStartFrom,
    sendLogStartTo,
    sendLogStats,
    sendLogStatusFilter,
    sendLogTypeFilter,
    sendLogsData,
    sendLogsError,
    sendLogsLoading,
    setActiveMailboxStep,
    setActiveMenu,
    setActiveSystemGroup,
    setAssignModalOpen,
    setAssignNotifyAssignee,
    setAssignReason,
    setAssignUserId,
    setAssignmentConfirmAction,
    setAssignmentEnabledFilter,
    setAssignmentKeyword,
    setAssignmentMatchTypeFilter,
    setAssignmentTestForm,
    setCalendarPreviewCreatedAt,
    setCalendarPreviewResolveHours,
    setCalendarPreviewResponseHours,
    setCloseConfirmed,
    setCloseModalOpen,
    setCloseReason,
    setDefaultSlaPolicy,
    setDefaultWorkCalendar,
    setFetchLogDetail,
    setHolidayKeyword,
    setHolidayMonth,
    setMailboxPage,
    setMsgFilter,
    setMsgSortAsc,
    setPriorityModalOpen,
    setPriorityReason,
    setPriorityValue,
    setRemarkDraft,
    setReplyContent,
    setReplyHtml,
    setRoleEnabledFilter,
    setRoleKeyword,
    setSelectedCustomerEmail,
    setSendLogDetail,
    setSlaPolicyConfirmAction,
    setSlaPolicyDefaultFilter,
    setSlaPolicyEnabledFilter,
    setSlaPolicyKeyword,
    setStatusModalOpen,
    setStatusReason,
    setStatusValue,
    setTemplateConfirmOpen,
    setTemplateKeyword,
    setTicketDetailTab,
    setTicketPage,
    setUserPage,
    setWorkCalendarConfirmAction,
    setWorkCalendarDefaultFilter,
    setWorkCalendarKeyword,
    showTicketDetailPage,
    slaCalendarCount,
    slaCalendarOptions,
    slaPoliciesData,
    slaPoliciesError,
    slaPoliciesLoading,
    slaPolicyActionLoading,
    slaPolicyConfirmAction,
    slaPolicyDefaultFilter,
    slaPolicyDirty,
    slaPolicyEnabledFilter,
    slaPolicyForm,
    slaPolicyKeyword,
    slaPolicySaving,
    slaPreview,
    slaPreviewBaseTime,
    slaResolveHoursInvalid,
    slaWarningInvalid,
    statusModalOpen,
    statusReason,
    statusSending,
    statusValue,
    submitAssignmentConfirm,
    submitConfirmAction,
    submitMailboxConfirm,
    submitRoleBase,
    submitRolePermissions,
    submitSlaPolicyConfirm,
    submitUserForm,
    submitWorkCalendarConfirm,
    systemGroups,
    templateConfirmOpen,
    templateDirty,
    templateForm,
    templateKeyword,
    templatePreview,
    templatePreviewLoading,
    templateSaving,
    templatesData,
    templatesError,
    templatesLoading,
    testMailboxConnection,
    ticketAttachments,
    ticketDetail,
    ticketDetailTab,
    ticketKeyword,
    ticketPage,
    ticketPageSize,
    ticketRule,
    ticketRuleConfirmOpen,
    ticketRuleDirty,
    ticketRuleError,
    ticketRuleForm,
    ticketRuleLoading,
    ticketRuleMessage,
    ticketRulePreviewLoading,
    ticketRuleSaving,
    ticketSlaBreachedOnly,
    ticketStats,
    ticketStatusTab,
    ticketsData,
    ticketsError,
    ticketsLoading,
    toggleAssignmentRule,
    toggleRoleEnabled,
    toggleRolePermission,
    toggleSlaPolicy,
    updateAssignmentForm,
    updateHolidayForm,
    updateMailboxForm,
    updateRoleForm,
    updateRoleScope,
    updateSlaPolicyForm,
    updateTemplateForm,
    updateTicketRuleForm,
    updateUserForm,
    updateWorkCalendarForm,
    uploadedFiles,
    uploadingFile,
    userEnabledFilter,
    userForm,
    userFormError,
    userFormMode,
    userFormOpen,
    userFormSubmitting,
    userKeyword,
    userPage,
    userPageSize,
    userRoleFilter,
    usersData,
    usersError,
    usersLoading,
    workCalendarActionLoading,
    workCalendarConfirmAction,
    workCalendarData,
    workCalendarDefaultFilter,
    workCalendarDirty,
    workCalendarError,
    workCalendarForm,
    workCalendarKeyword,
    workCalendarPageLoading,
    workCalendarSaving,
    workCalendarTimeInvalid,
    workCalendars,
    workCalendarsLoading,
    workdayLabel,
  } satisfies AppContentModel

  if (checkingSession) {
    return (
      <main className="session-check">
        <div className="session-check__panel">
          <div className="brand-mark">M</div>
          <p>正在恢复登录状态...</p>
        </div>
      </main>
    )
  }

  if (user) {
    return (
      <AppShell
        activeMenu={activeMenu}
        notificationsOpen={notificationsOpen}
        onGlobalSearch={handleGlobalSearch}
        onHelp={() =>
          setModal({
            title: '帮助',
            text: '帮助文档入口已固定在顶部栏。后续接入操作手册后，这里打开帮助抽屉或文档中心。',
          })}
        onLogout={() => void handleLogout()}
        onMenuChange={handleMenuChange}
        onNotificationsOpenChange={setNotificationsOpen}
        onProfileOpenChange={setProfileOpen}
        onSearchResultSelect={handleGlobalSearchResultSelect}
        onSearchKeywordChange={setSearchKeyword}
        onSidebarCollapsedChange={setSidebarCollapsed}
        profileOpen={profileOpen}
        searchInputRef={searchInputRef}
        searchKeyword={searchKeyword}
        searchResults={globalSearchMenuResults}
        sendPendingCount={sendPendingCount}
        sidebarCollapsed={sidebarCollapsed}
        user={user}
        visibleMenuGroups={visibleMenuGroups}
      >
        <AppContentRenderer activeMenu={activeMenu} model={contentModel} />

      </AppShell>
    )
  }

  return (
    <LoginPage
      account={account}
      accountError={accountError}
      formError={formError}
      modal={modal}
      onAccountChange={changeAccount}
      onModalChange={setModal}
      onPasswordChange={changePassword}
      onRememberMeChange={setRememberMe}
      onShowPasswordChange={setShowPassword}
      onSubmit={handleSubmit}
      password={password}
      passwordError={passwordError}
      rememberMe={rememberMe}
      showPassword={showPassword}
      submitting={submitting}
    />
  )
}

function customerTicketNoFromPath() {
  const prefix = '/customer/tickets/'
  const path = window.location.pathname
  if (!path.startsWith(prefix)) {
    return ''
  }
  return decodeURIComponent(path.slice(prefix.length).split('/')[0] || '').trim()
}

function App() {
  const customerTicketNo = customerTicketNoFromPath()
  if (customerTicketNo) {
    return <CustomerTicketLookupPage ticketNo={customerTicketNo} />
  }
  return <AdminApp />
}

export default App
