import { useState, type FormEvent } from 'react'
import { CopyIcon, PlusIcon, Trash2Icon } from 'lucide-react'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  addRange,
  copyMondayToWeekdays,
  countIntervals,
  dayName,
  fromWeekForm,
  hasErrors,
  MAX_INTERVALS,
  removeRange,
  setDayEnabled,
  STEP_MINUTES,
  toWeekForm,
  updateRange,
  validateWeek,
} from '@/lib/schedule'
import type { WorkingHoursInterval, WorkSchedule } from '@/services/schedules'

type WorkScheduleFormProps = {
  schedule: WorkSchedule
  onSubmit: (intervals: WorkingHoursInterval[]) => Promise<unknown>
  onCancel: () => void
  onReload: () => void
}

type ServerError =
  | {
      kind: 'conflict'
    }
  | {
      kind: 'message'
      message: string
    }

const alertClassName =
  'rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive'

function toServerError(error: unknown): ServerError {
  if (error instanceof ApiError) {
    if (error.status === 409) {
      return { kind: 'conflict' }
    }
    if (error.status === 404) {
      return {
        kind: 'message',
        message: 'This employee no longer exists. Close this window.',
      }
    }
    if (error.status === 400) {
      return {
        kind: 'message',
        message:
          error.problem.errors?.[0]?.message ??
          error.problem.detail ??
          "We couldn't save the hours. Check them and try again.",
      }
    }
  }
  return {
    kind: 'message',
    message: "We couldn't save the hours. Please try again.",
  }
}

export function WorkScheduleForm({
  schedule,
  onSubmit,
  onCancel,
  onReload,
}: WorkScheduleFormProps) {
  const [week, setWeek] = useState(() => toWeekForm(schedule.intervals))
  const [showErrors, setShowErrors] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [serverError, setServerError] = useState<ServerError | null>(null)

  const errors = validateWeek(week)
  const visibleErrors = showErrors ? errors : { days: {} }
  const canAddRange = countIntervals(week) < MAX_INTERVALS
  const canCopyMonday =
    week[0].enabled &&
    countIntervals(copyMondayToWeekdays(week)) <= MAX_INTERVALS

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError(null)
    if (hasErrors(errors)) {
      setShowErrors(true)
      return
    }
    setIsSubmitting(true)
    try {
      await onSubmit(fromWeekForm(week))
    } catch (error) {
      setServerError(toServerError(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4">
      {serverError?.kind === 'conflict' && (
        <div
          role="alert"
          className={`${alertClassName} flex flex-col items-start gap-2`}
        >
          Someone else saved these hours in the meantime. Reload to see the
          latest version.
          <Button type="button" variant="outline" size="sm" onClick={onReload}>
            Reload
          </Button>
        </div>
      )}
      {serverError?.kind === 'message' && (
        <p role="alert" className={alertClassName}>
          {serverError.message}
        </p>
      )}
      {showErrors && hasErrors(errors) && (
        <p role="alert" className={alertClassName}>
          {errors.form ?? 'Fix the highlighted days before saving.'}
        </p>
      )}

      <ul className="flex flex-col rounded-xl border border-border">
        {week.map((day) => {
          const name = dayName(day.dayOfWeek)
          const error = visibleErrors.days[day.dayOfWeek]
          const errorId = `schedule-error-${day.dayOfWeek}`
          return (
            <li
              key={day.dayOfWeek}
              className="flex flex-col gap-3 border-b border-border px-4 py-3 last:border-b-0 sm:flex-row sm:items-start"
            >
              <label className="flex h-10 cursor-pointer items-center gap-3 text-sm font-semibold sm:w-36 sm:shrink-0">
                <input
                  type="checkbox"
                  className="size-4 accent-foreground"
                  checked={day.enabled}
                  onChange={(event) =>
                    setWeek((current) =>
                      setDayEnabled(
                        current,
                        day.dayOfWeek,
                        event.target.checked,
                      ),
                    )
                  }
                />
                {name}
              </label>

              {day.enabled ? (
                <div className="flex flex-1 flex-col gap-2">
                  {day.ranges.map((range, index) => {
                    const number = index + 1
                    return (
                      <div key={range.key} className="flex items-center gap-2">
                        <Input
                          type="time"
                          step={STEP_MINUTES * 60}
                          required
                          aria-label={`${name} start ${number}`}
                          aria-invalid={error ? true : undefined}
                          aria-describedby={error ? errorId : undefined}
                          className="h-10 w-32"
                          value={range.startTime}
                          onChange={(event) =>
                            setWeek((current) =>
                              updateRange(
                                current,
                                day.dayOfWeek,
                                range.key,
                                'startTime',
                                event.target.value,
                              ),
                            )
                          }
                        />
                        <span aria-hidden="true">–</span>
                        <Input
                          type="time"
                          step={STEP_MINUTES * 60}
                          required
                          aria-label={`${name} end ${number}`}
                          aria-invalid={error ? true : undefined}
                          aria-describedby={error ? errorId : undefined}
                          className="h-10 w-32"
                          value={range.endTime}
                          onChange={(event) =>
                            setWeek((current) =>
                              updateRange(
                                current,
                                day.dayOfWeek,
                                range.key,
                                'endTime',
                                event.target.value,
                              ),
                            )
                          }
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon-sm"
                          aria-label={`Remove ${name} hours ${number}`}
                          onClick={() =>
                            setWeek((current) =>
                              removeRange(current, day.dayOfWeek, range.key),
                            )
                          }
                        >
                          <Trash2Icon aria-hidden="true" />
                        </Button>
                      </div>
                    )
                  })}
                  {error && (
                    <p
                      id={errorId}
                      className="text-sm font-medium text-destructive"
                    >
                      {error}
                    </p>
                  )}
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="self-start"
                    aria-label={`Add hours on ${name}`}
                    disabled={!canAddRange}
                    onClick={() =>
                      setWeek((current) => addRange(current, day.dayOfWeek))
                    }
                  >
                    <PlusIcon aria-hidden="true" />
                    Add hours
                  </Button>
                </div>
              ) : (
                <p className="flex h-10 items-center text-sm text-muted-foreground">
                  Day off
                </p>
              )}
            </li>
          )
        })}
      </ul>

      <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:items-center">
        <Button
          type="button"
          variant="outline"
          disabled={!canCopyMonday}
          onClick={() => setWeek(copyMondayToWeekdays)}
        >
          <CopyIcon aria-hidden="true" />
          Copy Monday to weekdays
        </Button>
        <div className="flex flex-col-reverse gap-2 sm:ml-auto sm:flex-row">
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving…' : 'Save hours'}
          </Button>
        </div>
      </div>
    </form>
  )
}
