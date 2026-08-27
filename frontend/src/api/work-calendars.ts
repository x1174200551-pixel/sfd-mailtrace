import { requestApi } from '../shared/api/request'
import type {
  Holiday,
  HolidayListResponse,
  NationalHolidayPresetResponse,
  WorkCalendar,
  WorkCalendarListResponse,
} from '../types/work-calendar'

export type WorkCalendarQuery = {
  enterpriseId?: number
  defaultCalendar?: string
  keyword?: string
}

export type WorkCalendarPayload = {
  enterpriseId: number
  calendarName: string
  defaultCalendar: boolean
  timezone: string
  workEndTime: string
  workStartTime: string
  workdays: number[]
}

export type HolidayQuery = {
  calendarId: number | string
  dateFrom?: string
  dateTo?: string
  keyword?: string
}

export type HolidayPayload = {
  calendarId: number | null
  holidayDate: string
  holidayName: string
}

function toQuery(params: Record<string, string | number | boolean | null | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value))
    }
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export const workCalendarApi = {
  list(params: WorkCalendarQuery = {}) {
    return requestApi<WorkCalendarListResponse>(`/api/v1/work-calendars${toQuery(params)}`)
  },

  save(calendarId: number | null, payload: WorkCalendarPayload) {
    return requestApi<WorkCalendar>(
      calendarId ? `/api/v1/work-calendars/${calendarId}` : '/api/v1/work-calendars',
      {
        method: calendarId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      },
    )
  },

  setDefault(calendarId: number) {
    return requestApi<WorkCalendar>(`/api/v1/work-calendars/${calendarId}/default`, {
      method: 'PATCH',
      body: JSON.stringify({ defaultCalendar: true }),
    })
  },

  delete(calendarId: number) {
    return requestApi<void>(`/api/v1/work-calendars/${calendarId}`, {
      method: 'DELETE',
    })
  },
}

export const holidayApi = {
  list(params: HolidayQuery) {
    return requestApi<HolidayListResponse>(`/api/v1/holidays${toQuery(params)}`)
  },

  save(holidayId: number | null, payload: HolidayPayload) {
    return requestApi<Holiday>(holidayId ? `/api/v1/holidays/${holidayId}` : '/api/v1/holidays', {
      method: holidayId ? 'PUT' : 'POST',
      body: JSON.stringify(payload),
    })
  },

  nationalPresets(year: number) {
    return requestApi<NationalHolidayPresetResponse>(`/api/v1/holidays/national-presets${toQuery({ year })}`)
  },

  delete(holidayId: number) {
    return requestApi<void>(`/api/v1/holidays/${holidayId}`, {
      method: 'DELETE',
    })
  },
}
