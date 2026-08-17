import dayjs from 'dayjs'
import type { HolidayFormState, WorkCalendarFormState } from '../types/work-calendar'

export const emptyWorkCalendarForm: WorkCalendarFormState = {
  id: null,
  calendarName: '',
  timezone: 'Asia/Shanghai',
  workdays: [1, 2, 3, 4, 5],
  workStartTime: '09:00',
  workEndTime: '18:00',
  defaultCalendar: false,
}

export const emptyHolidayForm: HolidayFormState = {
  id: null,
  calendarId: '',
  holidayDate: dayjs().format('YYYY-MM-DD'),
  holidayName: '',
}

export const weekdayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
