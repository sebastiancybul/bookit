import type { WorkingHoursInterval } from '@/services/schedules'

export const MAX_INTERVALS = 50
export const STEP_MINUTES = 5

const LATEST_END = 23 * 60 + 55
const DEFAULT_START = '09:00'
const DEFAULT_END = '17:00'
const NEXT_RANGE_GAP_MINUTES = 60
const NEXT_RANGE_LENGTH_MINUTES = 60
const WEEKDAYS = [2, 3, 4, 5]

export const dayNames = [
  'Monday',
  'Tuesday',
  'Wednesday',
  'Thursday',
  'Friday',
  'Saturday',
  'Sunday',
] as const

export type TimeRange = {
  key: string
  startTime: string
  endTime: string
}

export type DaySchedule = {
  dayOfWeek: number
  enabled: boolean
  ranges: TimeRange[]
}

export type WeekForm = DaySchedule[]

export type WeekErrors = {
  days: Record<number, string>
  form?: string
}

let lastKey = 0

function newRange(startTime: string, endTime: string): TimeRange {
  lastKey += 1
  return { key: `range-${lastKey}`, startTime, endTime }
}

export function dayName(dayOfWeek: number) {
  return dayNames[dayOfWeek - 1]
}

export function toMinutes(time: string): number | null {
  const match = /^(\d{2}):(\d{2})/.exec(time)
  if (!match) {
    return null
  }
  return Number(match[1]) * 60 + Number(match[2])
}

function toTime(minutes: number) {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return `${String(hours).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}

function byStart(a: TimeRange, b: TimeRange) {
  return a.startTime.localeCompare(b.startTime)
}

export function toWeekForm(intervals: WorkingHoursInterval[]): WeekForm {
  return dayNames.map((_, index) => {
    const dayOfWeek = index + 1
    const ranges = intervals
      .filter((interval) => interval.dayOfWeek === dayOfWeek)
      .map((interval) => newRange(interval.startTime, interval.endTime))
      .sort(byStart)
    return { dayOfWeek, enabled: ranges.length > 0, ranges }
  })
}

export function fromWeekForm(week: WeekForm): WorkingHoursInterval[] {
  return week
    .filter((day) => day.enabled)
    .flatMap((day) =>
      [...day.ranges].sort(byStart).map((range) => ({
        dayOfWeek: day.dayOfWeek,
        startTime: range.startTime,
        endTime: range.endTime,
      })),
    )
}

export function countIntervals(week: WeekForm) {
  return week
    .filter((day) => day.enabled)
    .reduce((total, day) => total + day.ranges.length, 0)
}

function updateDay(
  week: WeekForm,
  dayOfWeek: number,
  change: (day: DaySchedule) => DaySchedule,
): WeekForm {
  return week.map((day) => (day.dayOfWeek === dayOfWeek ? change(day) : day))
}

export function setDayEnabled(
  week: WeekForm,
  dayOfWeek: number,
  enabled: boolean,
): WeekForm {
  return updateDay(week, dayOfWeek, (day) => ({
    ...day,
    enabled,
    ranges:
      enabled && day.ranges.length === 0
        ? [newRange(DEFAULT_START, DEFAULT_END)]
        : day.ranges,
  }))
}

function nextRange(ranges: TimeRange[]): TimeRange {
  if (ranges.length === 0) {
    return newRange(DEFAULT_START, DEFAULT_END)
  }
  const ends = ranges
    .map((range) => toMinutes(range.endTime))
    .filter((end) => end !== null)
  if (ends.length < ranges.length) {
    return newRange('', '')
  }
  const latestEnd = Math.max(...ends)
  const start = latestEnd + NEXT_RANGE_GAP_MINUTES
  const end = start + NEXT_RANGE_LENGTH_MINUTES
  return end > LATEST_END
    ? newRange('', '')
    : newRange(toTime(start), toTime(end))
}

export function addRange(week: WeekForm, dayOfWeek: number): WeekForm {
  return updateDay(week, dayOfWeek, (day) => ({
    ...day,
    ranges: [...day.ranges, nextRange(day.ranges)],
  }))
}

export function removeRange(
  week: WeekForm,
  dayOfWeek: number,
  key: string,
): WeekForm {
  return updateDay(week, dayOfWeek, (day) => {
    const ranges = day.ranges.filter((range) => range.key !== key)
    return { ...day, enabled: ranges.length > 0, ranges }
  })
}

export function updateRange(
  week: WeekForm,
  dayOfWeek: number,
  key: string,
  field: 'startTime' | 'endTime',
  value: string,
): WeekForm {
  return updateDay(week, dayOfWeek, (day) => ({
    ...day,
    ranges: day.ranges.map((range) =>
      range.key === key ? { ...range, [field]: value } : range,
    ),
  }))
}

export function copyMondayToWeekdays(week: WeekForm): WeekForm {
  const monday = week[0]
  return week.map((day) =>
    WEEKDAYS.includes(day.dayOfWeek)
      ? {
          ...day,
          enabled: monday.enabled,
          ranges: monday.ranges.map((range) =>
            newRange(range.startTime, range.endTime),
          ),
        }
      : day,
  )
}

function validateDay(day: DaySchedule): string | undefined {
  const ranges: {
    start: number
    end: number
  }[] = []
  for (const range of day.ranges) {
    const start = toMinutes(range.startTime)
    const end = toMinutes(range.endTime)
    if (start === null || end === null) {
      return 'Enter a start and an end time'
    }
    ranges.push({ start, end })
  }
  if (
    ranges.some(
      (range) =>
        range.start % STEP_MINUTES !== 0 || range.end % STEP_MINUTES !== 0,
    )
  ) {
    return `Use ${STEP_MINUTES}-minute steps, like 9:05`
  }
  if (ranges.some((range) => range.start >= range.end)) {
    return 'Start time must be before end time'
  }
  const sorted = [...ranges].sort((a, b) => a.start - b.start)
  for (let index = 1; index < sorted.length; index++) {
    if (sorted[index].start < sorted[index - 1].end) {
      return 'Hours on this day overlap'
    }
  }
  return undefined
}

export function validateWeek(week: WeekForm): WeekErrors {
  const days: Record<number, string> = {}
  for (const day of week) {
    if (!day.enabled) {
      continue
    }
    const error = validateDay(day)
    if (error) {
      days[day.dayOfWeek] = error
    }
  }
  const form =
    countIntervals(week) > MAX_INTERVALS
      ? `You can set up to ${MAX_INTERVALS} time ranges per week`
      : undefined
  return { days, form }
}

export function hasErrors(errors: WeekErrors) {
  return errors.form !== undefined || Object.keys(errors.days).length > 0
}
