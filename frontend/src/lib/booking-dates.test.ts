import { describe, expect, it } from 'vitest'
import {
  addDays,
  formatDay,
  formatSlotTime,
  formatWeekRange,
  isBookableDay,
  startOfWeek,
  todayInZone,
  weekDays,
} from '@/lib/booking-dates'

describe('todayInZone', () => {
  it('returns the calendar date of the business, not of UTC', () => {
    const halfPastMidnightInWarsaw = new Date('2026-11-26T23:30:00Z')

    expect(todayInZone('Europe/Warsaw', halfPastMidnightInWarsaw)).toBe(
      '2026-11-27',
    )
    expect(todayInZone('UTC', halfPastMidnightInWarsaw)).toBe('2026-11-26')
  })
})

describe('addDays', () => {
  it('moves across month and year boundaries', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01')
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01')
    expect(addDays('2026-12-01', -1)).toBe('2026-11-30')
  })

  it('counts calendar days across a DST change', () => {
    expect(addDays('2027-03-27', 1)).toBe('2027-03-28')
    expect(addDays('2027-03-28', 1)).toBe('2027-03-29')
  })
})

describe('startOfWeek', () => {
  it('returns the Monday of the week', () => {
    expect(startOfWeek('2026-11-26')).toBe('2026-11-23')
    expect(startOfWeek('2026-11-23')).toBe('2026-11-23')
    expect(startOfWeek('2026-11-29')).toBe('2026-11-23')
    expect(startOfWeek('2027-01-01')).toBe('2026-12-28')
  })
})

describe('isBookableDay', () => {
  it('allows today up to 180 days ahead', () => {
    expect(isBookableDay('2026-11-25', '2026-11-26')).toBe(false)
    expect(isBookableDay('2026-11-26', '2026-11-26')).toBe(true)
    expect(isBookableDay('2027-05-25', '2026-11-26')).toBe(true)
    expect(isBookableDay('2027-05-26', '2026-11-26')).toBe(false)
  })
})

describe('weekDays', () => {
  it('returns seven days from Monday to Sunday', () => {
    expect(weekDays('2026-11-30')).toEqual([
      '2026-11-30',
      '2026-12-01',
      '2026-12-02',
      '2026-12-03',
      '2026-12-04',
      '2026-12-05',
      '2026-12-06',
    ])
  })
})

describe('formatDay', () => {
  it('returns the short weekday and the day of month', () => {
    expect(formatDay('2026-11-26')).toEqual({ weekday: 'Thu', day: '26' })
    expect(formatDay('2026-12-06')).toEqual({ weekday: 'Sun', day: '6' })
  })
})

describe('formatWeekRange', () => {
  it('shows the month once when the week stays in one month', () => {
    expect(formatWeekRange('2026-11-23')).toBe('23–29 Nov')
  })

  it('shows both months when the week crosses a month boundary', () => {
    expect(formatWeekRange('2026-11-30')).toBe('30 Nov – 6 Dec')
  })
})

describe('formatSlotTime', () => {
  it('shows the visit in the business time zone', () => {
    expect(formatSlotTime('2026-11-27T08:00:00Z', 'Europe/Warsaw')).toBe(
      'Fri 27 Nov, 09:00',
    )
    expect(formatSlotTime('2027-03-29T07:30:00Z', 'Europe/Warsaw')).toBe(
      'Mon 29 Mar, 09:30',
    )
  })
})
