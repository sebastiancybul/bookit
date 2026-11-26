import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'
import { cn } from 'cn'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { useAvailableSlots } from '@/hooks/useAvailableSlots'
import {
  addDays,
  formatDay,
  formatWeekRange,
  isBookableDay,
  MAX_DAYS_AHEAD,
  startOfWeek,
  weekDays,
} from '@/lib/booking-dates'
import {
  choiceClassName,
  choiceIdleClassName,
  choiceSelectedClassName,
} from '@/lib/styles'
import type { AvailableSlot } from '@/services/availability'

type SlotPickerProps = {
  slug: string
  offeringId: string
  employeeId: string
  timeZone: string
  today: string
  date: string
  onDateChange: (date: string) => void
  selectedStartTime: string | null
  onSelectSlot: (slot: AvailableSlot) => void
}

export function SlotPicker({
  slug,
  offeringId,
  employeeId,
  timeZone,
  today,
  date,
  onDateChange,
  selectedStartTime,
  onSelectSlot,
}: SlotPickerProps) {
  const weekStart = startOfWeek(date)
  const firstWeek = startOfWeek(today)
  const lastWeek = startOfWeek(addDays(today, MAX_DAYS_AHEAD))

  function goToWeek(week: string) {
    const firstBookable = weekDays(week).find((day) =>
      isBookableDay(day, today),
    )
    onDateChange(firstBookable ?? week)
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between gap-3">
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Previous week"
          disabled={weekStart <= firstWeek}
          onClick={() => goToWeek(addDays(weekStart, -7))}
        >
          <ChevronLeftIcon aria-hidden="true" />
        </Button>
        <span aria-live="polite" className="text-[15px] font-semibold">
          {formatWeekRange(weekStart)}
        </span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Next week"
          disabled={weekStart >= lastWeek}
          onClick={() => goToWeek(addDays(weekStart, 7))}
        >
          <ChevronRightIcon aria-hidden="true" />
        </Button>
      </div>

      <div role="group" aria-label="Day" className="grid grid-cols-7 gap-2">
        {weekDays(weekStart).map((day) => {
          const { weekday, day: dayOfMonth } = formatDay(day)
          const selected = day === date
          return (
            <button
              key={day}
              type="button"
              aria-label={`${weekday} ${dayOfMonth}`}
              aria-pressed={selected}
              disabled={!isBookableDay(day, today)}
              onClick={() => onDateChange(day)}
              className={cn(
                choiceClassName,
                'flex flex-col items-center gap-0.5 rounded-2xl py-3 disabled:cursor-not-allowed disabled:opacity-40',
                selected ? choiceSelectedClassName : choiceIdleClassName,
              )}
            >
              <span className="text-xs font-medium">{weekday}</span>
              <span className="text-base">{dayOfMonth}</span>
            </button>
          )
        })}
      </div>

      <DaySlots
        query={{ slug, offeringId, employeeId, date }}
        selectedStartTime={selectedStartTime}
        onSelectSlot={onSelectSlot}
      />

      <p className="text-[13px] text-muted-foreground">
        Times in {timeZone}, the business's time zone.
      </p>
    </div>
  )
}

type DaySlotsProps = {
  query: {
    slug: string
    offeringId: string
    employeeId: string
    date: string
  }
  selectedStartTime: string | null
  onSelectSlot: (slot: AvailableSlot) => void
}

function DaySlots({ query, selectedStartTime, onSelectSlot }: DaySlotsProps) {
  const { data: slots, isPending, error, refetch } = useAvailableSlots(query)

  if (!slots) {
    if (isPending) {
      return <p className="text-muted-foreground">Loading…</p>
    }
    const clientError = error instanceof ApiError && error.status < 500
    return (
      <div role="alert" className="flex flex-col items-start gap-3">
        <p className="text-muted-foreground">{describeError(error)}</p>
        {!clientError && (
          <Button type="button" variant="outline" onClick={() => refetch()}>
            Try again
          </Button>
        )}
      </div>
    )
  }

  if (slots.length === 0) {
    return (
      <p className="text-muted-foreground">
        No free times on this day. Try another day or specialist.
      </p>
    )
  }

  return (
    <div
      role="group"
      aria-label="Time"
      className="grid grid-cols-[repeat(auto-fill,minmax(5.5rem,1fr))] gap-2"
    >
      {slots.map((slot) => {
        const selected = slot.startTime === selectedStartTime
        return (
          <button
            key={slot.startTime}
            type="button"
            aria-pressed={selected}
            onClick={() => onSelectSlot(slot)}
            className={cn(
              choiceClassName,
              'h-11 rounded-full text-sm',
              selected ? choiceSelectedClassName : choiceIdleClassName,
            )}
          >
            {slot.localTime}
          </button>
        )
      })}
    </div>
  )
}

function describeError(error: Error | null): string {
  if (error instanceof ApiError && error.status === 404) {
    return 'This specialist no longer offers this service. Please choose another one.'
  }
  if (error instanceof ApiError && error.status < 500) {
    return "This day can't be booked. Please pick another day."
  }
  return "We couldn't load free times. Please try again."
}
