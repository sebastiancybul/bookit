export function todayInZone(timeZone: string, now: Date): string {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now)
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)?.value ?? ''
  return `${part('year')}-${part('month')}-${part('day')}`
}

export function addDays(date: string, days: number): string {
  const day = toUtcMidnight(date)
  day.setUTCDate(day.getUTCDate() + days)
  return day.toISOString().slice(0, 10)
}

export function startOfWeek(date: string): string {
  const daysSinceMonday = (toUtcMidnight(date).getUTCDay() + 6) % 7
  return addDays(date, -daysSinceMonday)
}

export const MAX_DAYS_AHEAD = 180

const WEEKDAY = new Intl.DateTimeFormat('en', {
  weekday: 'short',
  timeZone: 'UTC',
})
const MONTH = new Intl.DateTimeFormat('en', {
  month: 'short',
  timeZone: 'UTC',
})

export function isBookableDay(date: string, today: string): boolean {
  return date >= today && date <= addDays(today, MAX_DAYS_AHEAD)
}

export function weekDays(weekStart: string): string[] {
  return Array.from({ length: 7 }, (_, index) => addDays(weekStart, index))
}

export function formatDay(date: string): {
  weekday: string
  day: string
} {
  const day = toUtcMidnight(date)
  return { weekday: WEEKDAY.format(day), day: String(day.getUTCDate()) }
}

export function formatWeekRange(weekStart: string): string {
  const first = toUtcMidnight(weekStart)
  const last = toUtcMidnight(addDays(weekStart, 6))
  const firstMonth = MONTH.format(first)
  const lastMonth = MONTH.format(last)
  if (firstMonth === lastMonth) {
    return `${first.getUTCDate()}–${last.getUTCDate()} ${lastMonth}`
  }
  return `${first.getUTCDate()} ${firstMonth} – ${last.getUTCDate()} ${lastMonth}`
}

export function formatSlotTime(startTime: string, timeZone: string): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone,
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(startTime))
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)?.value ?? ''
  return `${part('weekday')} ${part('day')} ${part('month')}, ${part('hour')}:${part('minute')}`
}

function toUtcMidnight(date: string): Date {
  return new Date(`${date}T00:00:00Z`)
}
