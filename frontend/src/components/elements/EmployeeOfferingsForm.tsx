import { useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { FieldLegend, FieldSet } from '@/components/ui/field'
import { formatDuration } from '@/lib/format'
import type { Employee } from '@/services/employees'
import type { Offering } from '@/services/offerings'

type EmployeeOfferingsFormProps = {
  employee: Employee
  offerings: Offering[]
  onSubmit: (offeringIds: string[]) => Promise<unknown>
  onCancel: () => void
}

export function EmployeeOfferingsForm({
  employee,
  offerings,
  onSubmit,
  onCancel,
}: EmployeeOfferingsFormProps) {
  const [selected, setSelected] = useState(
    () =>
      new Set(
        employee.offeringIds.filter((id) =>
          offerings.some((offering) => offering.id === id),
        ),
      ),
  )
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [failed, setFailed] = useState(false)

  const toggle = (id: string) => {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setIsSubmitting(true)
    setFailed(false)
    try {
      await onSubmit([...selected])
    } catch {
      setFailed(true)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      {failed && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
        >
          We couldn't save the services. Refresh the page and try again.
        </p>
      )}

      {offerings.length === 0 ? (
        <p className="text-muted-foreground">
          Add services first, then assign them here.
        </p>
      ) : (
        <FieldSet>
          <FieldLegend className="sr-only">
            Services {employee.displayName} performs
          </FieldLegend>
          <ul className="flex max-h-80 flex-col overflow-y-auto rounded-xl border border-border">
            {offerings.map((offering) => (
              <li
                key={offering.id}
                className="border-b border-border last:border-b-0"
              >
                <label className="flex cursor-pointer items-center gap-3 px-4 py-3 text-sm">
                  <input
                    type="checkbox"
                    className="size-4 accent-foreground"
                    checked={selected.has(offering.id)}
                    onChange={() => toggle(offering.id)}
                  />
                  <span className="flex-1 font-medium">{offering.name}</span>
                  <span className="text-muted-foreground">
                    {formatDuration(offering.durationMinutes)}
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </FieldSet>
      )}

      <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting || offerings.length === 0}>
          {isSubmitting ? 'Saving…' : 'Save services'}
        </Button>
      </div>
    </form>
  )
}
