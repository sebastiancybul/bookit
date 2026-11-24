import { describe, expect, it } from 'vitest'
import {
  addRange,
  copyMondayToWeekdays,
  countIntervals,
  fromWeekForm,
  hasErrors,
  MAX_INTERVALS,
  removeRange,
  setDayEnabled,
  toWeekForm,
  updateRange,
  validateWeek,
  type WeekForm,
} from '@/lib/schedule'

function weekWith(ranges: Record<number, [string, string][]>): WeekForm {
  return toWeekForm(
    Object.entries(ranges).flatMap(([day, times]) =>
      times.map(([startTime, endTime]) => ({
        dayOfWeek: Number(day),
        startTime,
        endTime,
      })),
    ),
  )
}

function day(week: WeekForm, dayOfWeek: number) {
  return week[dayOfWeek - 1]
}

describe('toWeekForm and fromWeekForm', () => {
  it('builds seven days with sorted ranges and sends only enabled days', () => {
    const week = weekWith({
      1: [
        ['14:00', '18:00'],
        ['09:00', '13:00'],
      ],
      6: [['10:00', '14:00']],
    })

    expect(week).toHaveLength(7)
    expect(day(week, 1).ranges.map((range) => range.startTime)).toEqual([
      '09:00',
      '14:00',
    ])
    expect(day(week, 2).enabled).toBe(false)

    expect(fromWeekForm(setDayEnabled(week, 6, false))).toEqual([
      { dayOfWeek: 1, startTime: '09:00', endTime: '13:00' },
      { dayOfWeek: 1, startTime: '14:00', endTime: '18:00' },
    ])
  })

  it('gives every range a unique key', () => {
    const week = weekWith({ 1: [['09:00', '17:00']] })
    const copied = copyMondayToWeekdays(week)
    const keys = copied.flatMap((d) => d.ranges.map((range) => range.key))

    expect(new Set(keys).size).toBe(keys.length)
  })
})

describe('editing', () => {
  it('enables a day with default hours and keeps ranges when toggled back', () => {
    const empty = toWeekForm([])
    const enabled = setDayEnabled(empty, 3, true)

    expect(day(enabled, 3).ranges).toMatchObject([
      { startTime: '09:00', endTime: '17:00' },
    ])

    const edited = updateRange(
      enabled,
      3,
      day(enabled, 3).ranges[0].key,
      'endTime',
      '15:00',
    )
    const restored = setDayEnabled(setDayEnabled(edited, 3, false), 3, true)

    expect(day(restored, 3).ranges).toMatchObject([
      { startTime: '09:00', endTime: '15:00' },
    ])
  })

  it('adds the next range an hour after the latest end', () => {
    const week = addRange(weekWith({ 1: [['09:00', '13:00']] }), 1)

    expect(day(week, 1).ranges[1]).toMatchObject({
      startTime: '14:00',
      endTime: '15:00',
    })
  })

  it('adds an empty range when the next one would pass 23:55', () => {
    const week = addRange(weekWith({ 1: [['18:00', '23:00']] }), 1)

    expect(day(week, 1).ranges[1]).toMatchObject({
      startTime: '',
      endTime: '',
    })
  })

  it('adds an empty range when an existing end time is incomplete', () => {
    const week = weekWith({ 1: [['09:00', '']] })

    expect(day(addRange(week, 1), 1).ranges[1]).toMatchObject({
      startTime: '',
      endTime: '',
    })
  })

  it('turns the day off when its last range is removed', () => {
    const week = weekWith({ 2: [['09:00', '17:00']] })
    const removed = removeRange(week, 2, day(week, 2).ranges[0].key)

    expect(day(removed, 2)).toMatchObject({ enabled: false, ranges: [] })
  })

  it('copies Monday to Tuesday–Friday and leaves the weekend alone', () => {
    const week = weekWith({
      1: [
        ['09:00', '13:00'],
        ['14:00', '18:00'],
      ],
      5: [['08:00', '12:00']],
      6: [['10:00', '14:00']],
    })

    const copied = copyMondayToWeekdays(week)

    for (const dayOfWeek of [2, 3, 4, 5]) {
      expect(day(copied, dayOfWeek)).toMatchObject({
        enabled: true,
        ranges: [
          { startTime: '09:00', endTime: '13:00' },
          { startTime: '14:00', endTime: '18:00' },
        ],
      })
    }
    expect(day(copied, 6).ranges).toMatchObject([
      { startTime: '10:00', endTime: '14:00' },
    ])
    expect(day(copied, 7).enabled).toBe(false)
  })
})

describe('validateWeek', () => {
  it('accepts touching ranges', () => {
    const errors = validateWeek(
      weekWith({
        1: [
          ['09:00', '12:00'],
          ['12:00', '15:00'],
        ],
      }),
    )

    expect(hasErrors(errors)).toBe(false)
  })

  const invalidDays: [[string, string][], string][] = [
    [
      [
        ['09:00', '12:00'],
        ['11:55', '15:00'],
      ],
      'Hours on this day overlap',
    ],
    [
      [
        ['09:00', '17:00'],
        ['10:00', '11:00'],
      ],
      'Hours on this day overlap',
    ],
    [[['09:03', '12:00']], 'Use 5-minute steps, like 9:05'],
    [[['12:00', '12:00']], 'Start time must be before end time'],
    [[['13:00', '12:00']], 'Start time must be before end time'],
    [[['', '12:00']], 'Enter a start and an end time'],
  ]

  it.each(invalidDays)('rejects %j with "%s"', (ranges, message) => {
    const errors = validateWeek(weekWith({ 4: ranges }))

    expect(errors.days).toEqual({ 4: message })
  })

  it('ignores invalid ranges on a day that is off', () => {
    const week = setDayEnabled(weekWith({ 1: [['13:00', '12:00']] }), 1, false)

    expect(hasErrors(validateWeek(week))).toBe(false)
  })

  it(`rejects more than ${MAX_INTERVALS} ranges in the week`, () => {
    const eightPerDay = Array.from({ length: 8 }, (_, index) => {
      const hour = String(index * 2 + 6).padStart(2, '0')
      return [`${hour}:00`, `${hour}:30`] as [string, string]
    })
    const week = weekWith({
      1: eightPerDay,
      2: eightPerDay,
      3: eightPerDay,
      4: eightPerDay,
      5: eightPerDay,
      6: eightPerDay,
      7: eightPerDay,
    })

    expect(countIntervals(week)).toBe(56)
    expect(validateWeek(week)).toEqual({
      days: {},
      form: `You can set up to ${MAX_INTERVALS} time ranges per week`,
    })
  })
})
