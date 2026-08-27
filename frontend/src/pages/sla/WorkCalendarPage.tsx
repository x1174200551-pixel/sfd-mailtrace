import { Alert, Button, Card, Checkbox, Col, DatePicker, Descriptions, Empty, Input, Row, Select, Space, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import { Plus, RefreshCw, ShieldCheck } from 'lucide-react'
import type { Dayjs } from 'dayjs'
import { weekdayNames } from '../../constants/work-calendars'
import type {
  CalendarSlaExample,
  Holiday,
  HolidayFormState,
  HolidayListResponse,
  MonthCell,
  WorkCalendar,
  WorkCalendarConfirmAction,
  WorkCalendarFormState,
  WorkCalendarListResponse,
  WorkCalendarPolicy,
} from '../../types/work-calendar'
import type { EnterpriseOption } from '../../types/enterprise'

type WorkCalendarPageProps = {
  actionLoading: boolean
  calendarPreviewCreatedAtValue: Dayjs
  calendarPreviewResponseHours: string
  calendarPreviewResponseHoursValue: number
  calendarPreviewResolveHours: string
  calendarPreviewResolveHoursValue: number
  calendarSlaExample: CalendarSlaExample
  calendarSlaPolicies: WorkCalendarPolicy[]
  canCreateHolidays: boolean
  canCreateWorkCalendars: boolean
  canDeleteHolidays: boolean
  canDeleteWorkCalendars: boolean
  canImportHolidays: boolean
  canReadWorkCalendars: boolean
  canUpdateHolidays: boolean
  confirmAction: WorkCalendarConfirmAction
  holidayDateValue: Dayjs | null
  holidayDirty: boolean
  holidayForm: HolidayFormState
  holidayImportYear: number
  holidayImporting: boolean
  holidayKeyword: string
  holidayMonthValue: Dayjs | null
  holidayRecords: Holiday[]
  holidaySaving: boolean
  holidaysData: HolidayListResponse | null
  holidaysError: string
  holidaysLoading: boolean
  monthCells: MonthCell[]
  onCalendarPreviewCreatedAtChange: (value: string) => void
  onCalendarPreviewResponseHoursChange: (value: string) => void
  onCalendarPreviewResolveHoursChange: (value: string) => void
  onCancelConfirm: () => void
  onConfirmDelete: () => void
  onFetchAll: () => void
  onFetchHolidays: () => void
  onFetchWorkCalendars: () => void
  onHolidayKeywordChange: (value: string) => void
  onHolidayMonthChange: (value: string) => void
  onImportNationalHolidays: () => void
  onOpenCreateHoliday: () => void
  onOpenCreateWorkCalendar: () => void
  onRequestDeleteCalendar: (calendar: WorkCalendar) => void
  onRequestDeleteHoliday: (holiday: Holiday) => void
  onResetCalendarPreview: () => void
  onResetWorkCalendarFilters: () => void
  onSaveHoliday: () => void
  onSaveWorkCalendar: () => void
  onSelectHoliday: (holiday: Holiday) => void
  onSelectWorkCalendar: (calendar: WorkCalendar) => void
  onSetDefaultWorkCalendar: (calendar: WorkCalendar) => void
  onUpdateHolidayForm: (patch: Partial<HolidayFormState>) => void
  onUpdateWorkCalendarForm: (patch: Partial<WorkCalendarFormState>) => void
  onWorkCalendarDefaultFilterChange: (value: string) => void
  onWorkCalendarKeywordChange: (value: string) => void
  selectedCalendar: WorkCalendar | null
  timeInvalid: boolean
  workCalendarData: WorkCalendarListResponse | null
  workCalendarDefaultFilter: string
  workCalendarEnterpriseFilter: string
  workCalendarEnterpriseOptions: EnterpriseOption[]
  workCalendarDirty: boolean
  workCalendarError: string
  workCalendarForm: WorkCalendarFormState
  workCalendarKeyword: string
  workCalendarSaving: boolean
  workCalendarsLoading: boolean
  workdayLabel: (workdays?: number[]) => string
  onWorkCalendarEnterpriseFilterChange: (value: string) => void
}

function weekdayIndex(date: MonthCell['date']) {
  const day = date.day()
  return day === 0 ? 6 : day - 1
}

export function WorkCalendarPage({
  actionLoading,
  calendarPreviewCreatedAtValue,
  calendarPreviewResponseHours,
  calendarPreviewResponseHoursValue,
  calendarPreviewResolveHours,
  calendarPreviewResolveHoursValue,
  calendarSlaExample,
  calendarSlaPolicies,
  canCreateHolidays,
  canCreateWorkCalendars,
  canDeleteHolidays,
  canDeleteWorkCalendars,
  canImportHolidays,
  canReadWorkCalendars,
  canUpdateHolidays,
  confirmAction,
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
  monthCells,
  onCalendarPreviewCreatedAtChange,
  onCalendarPreviewResponseHoursChange,
  onCalendarPreviewResolveHoursChange,
  onCancelConfirm,
  onConfirmDelete,
  onFetchAll,
  onFetchHolidays,
  onFetchWorkCalendars,
  onHolidayKeywordChange,
  onHolidayMonthChange,
  onImportNationalHolidays,
  onOpenCreateHoliday,
  onOpenCreateWorkCalendar,
  onRequestDeleteCalendar,
  onRequestDeleteHoliday,
  onResetCalendarPreview,
  onResetWorkCalendarFilters,
  onSaveHoliday,
  onSaveWorkCalendar,
  onSelectHoliday,
  onSelectWorkCalendar,
  onSetDefaultWorkCalendar,
  onUpdateHolidayForm,
  onUpdateWorkCalendarForm,
  onWorkCalendarDefaultFilterChange,
  onWorkCalendarKeywordChange,
  selectedCalendar,
  timeInvalid,
  workCalendarData,
  workCalendarDefaultFilter,
  workCalendarEnterpriseFilter,
  workCalendarEnterpriseOptions,
  workCalendarDirty,
  workCalendarError,
  workCalendarForm,
  workCalendarKeyword,
  workCalendarSaving,
  workCalendarsLoading,
  workdayLabel,
  onWorkCalendarEnterpriseFilterChange,
}: WorkCalendarPageProps) {
  const workCalendarRecords = workCalendarData?.records ?? []
  const workCalendarSummary = workCalendarData?.summary
  const selectedCalendarPolicyCount = selectedCalendar
    ? calendarSlaPolicies.filter((policy) => policy.calendarId === selectedCalendar.id).length
    : 0
  const totalCalendarPolicyCount = calendarSlaPolicies.length
  const selectedCalendarHolidayCount = holidayRecords.filter((holiday) => holiday.calendarId === selectedCalendar?.id).length
  const deleteBlockedReason = selectedCalendar?.defaultCalendar
    ? '默认日历不可删除'
    : selectedCalendarPolicyCount > 0
      ? `已被 ${selectedCalendarPolicyCount} 条 SLA 策略引用`
      : selectedCalendarHolidayCount > 0
        ? `当前月份已配置 ${selectedCalendarHolidayCount} 个节假日`
        : ''

  return (
    <>
      <section className="app-content" aria-label="工作日历">
        <div className="content-title">
          <div>
            <h1>工作日历</h1>
            <p>维护 SLA 计算使用的工作日、工作时段和节假日；策略绑定后按该日历计算响应与解决截止时间。</p>
          </div>
          <div className="content-actions">
            <button disabled={workCalendarsLoading || holidaysLoading} onClick={onFetchAll} type="button">
              <RefreshCw size={16} />
              刷新
            </button>
            <button
              className="primary-action"
              disabled={!canCreateWorkCalendars}
              onClick={onOpenCreateWorkCalendar}
              type="button"
            >
              <Plus size={16} />
              新建日历
            </button>
            <button disabled={!canCreateHolidays} onClick={onOpenCreateHoliday} type="button">
              <Plus size={16} />
              新增节假日
            </button>
          </div>
        </div>

        {!canReadWorkCalendars ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无工作日历管理权限</strong>
            <p>当前账号没有工作日历查看权限；日历与节假日维护动作由独立权限控制。</p>
          </div>
        ) : (
          <>
            <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">日历总数</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{workCalendarSummary?.totalCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">工作日历可被 SLA 策略绑定</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">默认日历</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{workCalendarSummary?.defaultCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">默认日历不可直接删除</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">本月节假日</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{holidaysData?.summary.totalCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">按所选日历和月份统计</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">绑定策略</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{totalCalendarPolicyCount || '--'}</Typography.Title>
                  <Typography.Text type="secondary">由 SLA 策略列表按日历派生</Typography.Text>
                </Card>
              </Col>
            </Row>

            <Alert
              showIcon
              type="info"
              style={{ marginBottom: 16 }}
              title="工作日历和节假日保存后只影响后续 SLA 计算；历史工单已有响应截止和解决截止不自动重算。"
            />

            {workCalendarError && (
              <Alert
                showIcon
                type="error"
                style={{ marginBottom: 16 }}
                title={workCalendarError}
                action={<Button size="small" onClick={onFetchWorkCalendars}>重试</Button>}
              />
            )}

            {holidaysError && (
              <Alert
                showIcon
                type="error"
                style={{ marginBottom: 16 }}
                title={holidaysError}
                action={<Button size="small" onClick={onFetchHolidays}>重试</Button>}
              />
            )}

            <Row gutter={[16, 16]}>
              <Col xs={24} xl={8}>
                <Card title="日历列表">
                  <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                    <Select
                      style={{ width: 180 }}
                      value={workCalendarEnterpriseFilter}
                      onChange={onWorkCalendarEnterpriseFilterChange}
                      options={workCalendarEnterpriseOptions.map((enterprise) => ({ value: String(enterprise.id), label: enterprise.enterpriseName }))}
                    />
                    <Input
                      allowClear
                      prefix={<SearchOutlined />}
                      placeholder="日历名称"
                      style={{ width: 180 }}
                      value={workCalendarKeyword}
                      onChange={(event) => onWorkCalendarKeywordChange(event.target.value)}
                      onPressEnter={() => void onFetchWorkCalendars()}
                    />
                    <Select
                      style={{ width: 126 }}
                      value={workCalendarDefaultFilter}
                      onChange={onWorkCalendarDefaultFilterChange}
                      options={[
                        { value: 'ALL', label: '全部日历' },
                        { value: 'true', label: '默认日历' },
                        { value: 'false', label: '非默认' },
                      ]}
                    />
                    <Button onClick={onResetWorkCalendarFilters}>清空筛选</Button>
                  </Space>

                  <Table<WorkCalendar>
                    rowKey="id"
                    size="middle"
                    loading={workCalendarsLoading}
                    dataSource={workCalendarRecords}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty description="还没有工作日历" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                          <Button type="primary" onClick={onOpenCreateWorkCalendar}>新建日历</Button>
                        </Empty>
                      ),
                    }}
                    rowClassName={(record) => record.id === workCalendarForm.id ? 'ant-table-row-selected' : ''}
                    onRow={(record) => ({
                      onClick: () => onSelectWorkCalendar(record),
                    })}
                    columns={[
                      {
                        title: '日历',
                        dataIndex: 'calendarName',
                        render: (_value: string, record: WorkCalendar) => {
                          const policyCount = calendarSlaPolicies.filter((policy) => policy.calendarId === record.id).length
                          return (
                            <Space orientation="vertical" size={4}>
                              <Space wrap>
                                <Typography.Text strong>{record.calendarName}</Typography.Text>
                                {record.defaultCalendar && <Tag color="blue">默认</Tag>}
                                {policyCount > 0 && <Tag color="gold">绑定 {policyCount} 条策略</Tag>}
                              </Space>
                              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                {record.timezone} · {workdayLabel(record.workdays)} · {record.workStartTime}-{record.workEndTime}
                              </Typography.Text>
                              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                更新：{record.updatedAt ? record.updatedAt.replace('T', ' ').slice(0, 16) : '-'}
                              </Typography.Text>
                            </Space>
                          )
                        },
                      },
                      {
                        title: '操作',
                        width: 92,
                        render: (_value: unknown, record: WorkCalendar) => (
                          <Space onClick={(event) => event.stopPropagation()}>
                            <Button
                              size="small"
                              disabled={record.defaultCalendar || actionLoading}
                              onClick={() => void onSetDefaultWorkCalendar(record)}
                            >
                              默认
                            </Button>
                          </Space>
                        ),
                      },
                    ]}
                  />
                </Card>
              </Col>

              <Col xs={24} xl={8}>
                <Card
                  title="日历编辑"
                  extra={
                    workCalendarDirty
                      ? <Tag color="orange">有未保存修改</Tag>
                      : selectedCalendar
                        ? <Tag color="green">已保存</Tag>
                        : <Tag>新建草稿</Tag>
                  }
                >
                  <Row gutter={[12, 12]}>
                    <Col span={24}>
                      <Typography.Text strong>所属企业</Typography.Text>
                      <Select
                        value={workCalendarForm.enterpriseId || undefined}
                        onChange={(value) => onUpdateWorkCalendarForm({ enterpriseId: value })}
                        options={workCalendarEnterpriseOptions.map((enterprise) => ({ value: String(enterprise.id), label: enterprise.enterpriseName }))}
                        style={{ width: '100%', marginTop: 8 }}
                      />
                    </Col>
                    <Col span={24}>
                      <Typography.Text strong>日历名称</Typography.Text>
                      <Input
                        value={workCalendarForm.calendarName}
                        onChange={(event) => onUpdateWorkCalendarForm({ calendarName: event.target.value })}
                        placeholder="客服工作日历"
                        style={{ marginTop: 8 }}
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>calendarName，最多 64 字。</Typography.Text>
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>时区</Typography.Text>
                      <Select
                        showSearch
                        value={workCalendarForm.timezone}
                        onChange={(value) => onUpdateWorkCalendarForm({ timezone: value })}
                        options={[
                          { value: 'Asia/Shanghai', label: 'Asia/Shanghai' },
                          { value: 'Asia/Singapore', label: 'Asia/Singapore' },
                          { value: 'UTC', label: 'UTC' },
                        ]}
                        style={{ width: '100%', marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>默认日历</Typography.Text>
                      <Select
                        value={String(workCalendarForm.defaultCalendar)}
                        onChange={(value) => onUpdateWorkCalendarForm({ defaultCalendar: value === 'true' })}
                        options={[
                          { value: 'true', label: '设为默认' },
                          { value: 'false', label: '非默认' },
                        ]}
                        style={{ width: '100%', marginTop: 8 }}
                      />
                    </Col>
                    <Col span={24}>
                      <Typography.Text strong>工作日</Typography.Text>
                      <div style={{ marginTop: 8 }}>
                        <Checkbox.Group
                          value={workCalendarForm.workdays}
                          options={weekdayNames.map((label, index) => ({ label, value: index + 1 }))}
                          onChange={(values) => onUpdateWorkCalendarForm({ workdays: values.map(Number).sort((a, b) => a - b) })}
                        />
                      </div>
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>workdays，1=周一，7=周日，至少选择一天。</Typography.Text>
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>工作开始时间</Typography.Text>
                      <Input
                        type="time"
                        value={workCalendarForm.workStartTime}
                        onChange={(event) => onUpdateWorkCalendarForm({ workStartTime: event.target.value })}
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>工作结束时间</Typography.Text>
                      <Input
                        type="time"
                        status={timeInvalid ? 'error' : undefined}
                        value={workCalendarForm.workEndTime}
                        onChange={(event) => onUpdateWorkCalendarForm({ workEndTime: event.target.value })}
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                    <Col span={24}>
                      <Alert
                        showIcon
                        type={timeInvalid || workCalendarForm.workdays.length === 0 ? 'error' : 'info'}
                        title={timeInvalid || workCalendarForm.workdays.length === 0 ? '日历校验未通过' : '保存影响范围'}
                        description={
                          timeInvalid
                            ? '工作开始时间必须早于工作结束时间。'
                            : workCalendarForm.workdays.length === 0
                              ? '至少选择一个工作日。'
                              : '保存后只影响后续 SLA 计算，历史工单已有截止时间保持不变。'
                        }
                      />
                    </Col>
                  </Row>

                  <Space style={{ marginTop: 16 }} wrap>
                    <Button disabled={!canCreateWorkCalendars} onClick={onOpenCreateWorkCalendar}>新建草稿</Button>
                    <Button
                      type="primary"
                      loading={workCalendarSaving}
                      disabled={!canCreateWorkCalendars || !workCalendarForm.enterpriseId || !workCalendarForm.calendarName.trim() || workCalendarForm.workdays.length === 0 || timeInvalid}
                      onClick={() => void onSaveWorkCalendar()}
                    >
                      保存日历
                    </Button>
                    <Button
                      danger
                      disabled={!canDeleteWorkCalendars || !selectedCalendar || Boolean(deleteBlockedReason)}
                      icon={<DeleteOutlined />}
                      onClick={() => selectedCalendar && onRequestDeleteCalendar(selectedCalendar)}
                    >
                      删除日历
                    </Button>
                    {deleteBlockedReason && <Tag color="orange">{deleteBlockedReason}</Tag>}
                  </Space>
                </Card>
              </Col>

              <Col xs={24} xl={8}>
                <Card
                  title="节假日"
                  extra={
                    <Button
                      size="small"
                      loading={holidayImporting}
                      disabled={!canImportHolidays || !workCalendarForm.id || holidaysLoading}
                      onClick={() => void onImportNationalHolidays()}
                    >
                      导入 {holidayImportYear} 法定节假日
                    </Button>
                  }
                >
                  <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                    <DatePicker
                      picker="month"
                      value={holidayMonthValue}
                      onChange={(value) => {
                        if (value) onHolidayMonthChange(value.format('YYYY-MM'))
                      }}
                      allowClear={false}
                    />
                    <Input
                      allowClear
                      placeholder="节假日名称"
                      style={{ width: 150 }}
                      value={holidayKeyword}
                      onChange={(event) => onHolidayKeywordChange(event.target.value)}
                      onPressEnter={() => void onFetchHolidays()}
                    />
                    <Button disabled={!canCreateHolidays} onClick={onOpenCreateHoliday}>新增</Button>
                  </Space>
                  <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12, marginBottom: 12 }}>
                    快捷导入从三方节假日接口获取所选年份放假日期；补班日当前不单独建模。
                  </Typography.Text>

                  <Table<Holiday>
                    rowKey="id"
                    size="small"
                    loading={holidaysLoading}
                    dataSource={holidayRecords}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty description="当前月份暂无节假日" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                          <Button disabled={!canCreateHolidays} type="primary" onClick={onOpenCreateHoliday}>新增节假日</Button>
                        </Empty>
                      ),
                    }}
                    onRow={(record) => ({
                      onClick: () => {
                        if (canUpdateHolidays) onSelectHoliday(record)
                      },
                    })}
                    columns={[
                      { title: '日期', dataIndex: 'holidayDate', width: 112 },
                      { title: '名称', dataIndex: 'holidayName' },
                      {
                        title: '操作',
                        width: 120,
                        render: (_value: unknown, record: Holiday) => (
                          <Space onClick={(event) => event.stopPropagation()}>
                            <Button disabled={!canUpdateHolidays} size="small" onClick={() => onSelectHoliday(record)}>编辑</Button>
                            <Button
                              size="small"
                              danger
                              disabled={!canDeleteHolidays || actionLoading}
                              onClick={() => onRequestDeleteHoliday(record)}
                            >
                              删除
                            </Button>
                          </Space>
                        ),
                      },
                    ]}
                  />

                  <Card size="small" title="新增/编辑节假日" style={{ marginTop: 16 }}>
                    <Space orientation="vertical" size={10} style={{ width: '100%' }}>
                      <DatePicker
                        value={holidayDateValue}
                        onChange={(value) => onUpdateHolidayForm({ holidayDate: value ? value.format('YYYY-MM-DD') : '' })}
                        style={{ width: '100%' }}
                        allowClear={false}
                      />
                      <Input
                        value={holidayForm.holidayName}
                        onChange={(event) => onUpdateHolidayForm({ holidayName: event.target.value })}
                        placeholder="国庆节"
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>字段按当前后端：holidayDate + holidayName。</Typography.Text>
                      <Space wrap>
                        <Button disabled={!canCreateHolidays} onClick={onOpenCreateHoliday}>新建草稿</Button>
                        <Button
                          type="primary"
                          loading={holidaySaving}
                          disabled={!(holidayForm.id ? canUpdateHolidays : canCreateHolidays) || !holidayForm.calendarId || !holidayForm.holidayDate || !holidayForm.holidayName.trim()}
                          onClick={() => void onSaveHoliday()}
                        >
                          保存节假日
                        </Button>
                        {holidayDirty && <Tag color="orange">有未保存修改</Tag>}
                      </Space>
                    </Space>
                  </Card>
                </Card>
              </Col>
            </Row>

            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
              <Col xs={24} xl={14}>
                <Card
                  title="月历预览"
                  extra={
                    <DatePicker
                      picker="month"
                      size="small"
                      value={holidayMonthValue}
                      onChange={(value) => {
                        if (value) onHolidayMonthChange(value.format('YYYY-MM'))
                      }}
                      allowClear={false}
                      style={{ width: 128 }}
                    />
                  }
                >
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, minmax(0, 1fr))', gap: 8 }}>
                    {weekdayNames.map((name) => (
                      <Typography.Text key={name} type="secondary" style={{ textAlign: 'center', fontSize: 12, fontWeight: 700 }}>
                        {name}
                      </Typography.Text>
                    ))}
                    {monthCells.map((cell) => (
                      <div
                        key={cell.dateKey}
                        style={{
                          minHeight: 58,
                          border: cell.isToday ? '2px solid #2563eb' : cell.isWorkday ? '1px solid #bfdbfe' : '1px solid #e5e7eb',
                          borderRadius: 8,
                          padding: 8,
                          background: !cell.inMonth ? '#f8fafc' : cell.holidayName ? '#fff7ed' : cell.isWorkday ? '#dbeafe' : '#fff',
                          color: !cell.inMonth ? '#94a3b8' : '#111827',
                        }}
                      >
                        <Typography.Text strong>{cell.date.date()}</Typography.Text>
                        <div style={{ marginTop: 4 }}>
                          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                            {cell.holidayName || (cell.isWorkday && selectedCalendar ? `${selectedCalendar.workStartTime}-${selectedCalendar.workEndTime}` : weekdayNames[weekdayIndex(cell.date)])}
                          </Typography.Text>
                        </div>
                      </div>
                    ))}
                  </div>
                </Card>
              </Col>
              <Col xs={24} xl={10}>
                <Card
                  title="SLA 计算示例"
                  extra={(
                    <Space size={8}>
                      <Tag color="green">工作小时</Tag>
                      <Button size="small" onClick={onResetCalendarPreview}>重置</Button>
                    </Space>
                  )}
                >
                  <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>建单时间</Typography.Text>
                      <DatePicker
                        showTime={{ format: 'HH:mm' }}
                        value={calendarPreviewCreatedAtValue}
                        onChange={(value) => {
                          if (value) onCalendarPreviewCreatedAtChange(value.format('YYYY-MM-DDTHH:mm:ss'))
                        }}
                        format="YYYY-MM-DD HH:mm"
                        allowClear={false}
                        style={{ width: '100%', marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={12} md={6}>
                      <Typography.Text strong>响应小时</Typography.Text>
                      <Input
                        type="number"
                        min={1}
                        step={0.5}
                        value={calendarPreviewResponseHours}
                        onChange={(event) => onCalendarPreviewResponseHoursChange(event.target.value)}
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={12} md={6}>
                      <Typography.Text strong>解决小时</Typography.Text>
                      <Input
                        type="number"
                        min={1}
                        step={0.5}
                        value={calendarPreviewResolveHours}
                        onChange={(event) => onCalendarPreviewResolveHoursChange(event.target.value)}
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                  </Row>
                  <Descriptions
                    bordered
                    size="small"
                    column={1}
                    items={[
                      { key: 'calendar', label: '工作日历', children: selectedCalendar?.calendarName || '未选择' },
                      { key: 'created', label: '建单时间', children: calendarPreviewCreatedAtValue.format('YYYY-MM-DD HH:mm') },
                      { key: 'start', label: '起算时间', children: calendarSlaExample.startAt.format('YYYY-MM-DD HH:mm') },
                      { key: 'response', label: `${calendarPreviewResponseHoursValue} 工作小时响应截止`, children: calendarSlaExample.responseDeadline.format('YYYY-MM-DD HH:mm') },
                      { key: 'resolve', label: `${calendarPreviewResolveHoursValue} 工作小时解决截止`, children: calendarSlaExample.resolveDeadline.format('YYYY-MM-DD HH:mm') },
                    ]}
                  />
                  <Alert
                    showIcon
                    type="success"
                    style={{ marginTop: 16 }}
                    title="实际截止时间以后端 SlaDeadlineService 写入结果为准。"
                  />
                </Card>
              </Col>
            </Row>
          </>
        )}
      </section>

      {confirmAction && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="work-calendar-confirm-title">
          <div className="confirm-modal">
            <h3 id="work-calendar-confirm-title">
              {confirmAction.type === 'delete-calendar' ? '删除工作日历' : '删除节假日'}
            </h3>
            <p>
              {confirmAction.type === 'delete-calendar'
                ? '删除后该日历不再用于后续 SLA 计算，历史工单已有截止时间保持不变。'
                : '删除后该日期不再作为后续 SLA 计算的节假日，历史工单已有截止时间保持不变。'}
            </p>
            <div className="confirm-target">
              <strong>
                {confirmAction.type === 'delete-calendar'
                  ? confirmAction.calendar.calendarName
                  : confirmAction.holiday.holidayName}
              </strong>
              <span>
                {confirmAction.type === 'delete-calendar'
                  ? `${workdayLabel(confirmAction.calendar.workdays)} · ${confirmAction.calendar.workStartTime}-${confirmAction.calendar.workEndTime}`
                  : confirmAction.holiday.holidayDate}
              </span>
            </div>
            <div className="user-modal__foot">
              <button disabled={actionLoading} onClick={onCancelConfirm} type="button">
                取消
              </button>
              <button className="primary-action" disabled={actionLoading} onClick={onConfirmDelete} type="button">
                {actionLoading ? '删除中...' : '确认删除'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
