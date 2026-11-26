import { cn } from 'cn'
import { PersonAvatar } from '@/components/elements/PersonAvatar'
import { choiceClassName, choiceIdleClassName } from '@/lib/styles'
import type { PublicEmployee } from '@/services/employees'

type SpecialistPickerProps = {
  employees: PublicEmployee[]
  offeringId: string
  selectedEmployeeId: string | null
  onSelect: (employee: PublicEmployee) => void
}

export function SpecialistPicker({
  employees,
  offeringId,
  selectedEmployeeId,
  onSelect,
}: SpecialistPickerProps) {
  const specialists = employees.filter((employee) =>
    employee.offeringIds.includes(offeringId),
  )

  if (specialists.length === 0) {
    return (
      <p className="text-muted-foreground">
        No specialist offers this service yet. Please choose another service.
      </p>
    )
  }

  return (
    <div
      role="group"
      aria-label="Specialist"
      className="grid grid-cols-[repeat(auto-fill,minmax(13rem,1fr))] gap-2.5"
    >
      {specialists.map((employee) => {
        const selected = employee.id === selectedEmployeeId
        return (
          <button
            key={employee.id}
            type="button"
            aria-pressed={selected}
            onClick={() => onSelect(employee)}
            className={cn(
              choiceClassName,
              'flex min-h-16 items-center gap-3 rounded-2xl px-3.5 py-3 text-left',
              selected
                ? 'border-foreground bg-background ring-3 ring-muted'
                : choiceIdleClassName,
            )}
          >
            <PersonAvatar name={employee.displayName} seed={employee.id} />
            <span className="flex min-w-0 flex-col">
              <span className="truncate">{employee.displayName}</span>
              {employee.position && (
                <span className="truncate text-sm font-normal text-muted-foreground">
                  {employee.position}
                </span>
              )}
            </span>
          </button>
        )
      })}
    </div>
  )
}
