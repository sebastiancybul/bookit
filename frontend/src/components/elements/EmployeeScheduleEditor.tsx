import { useState } from 'react'
import { ApiError } from '@/api/client'
import { WorkScheduleForm } from '@/components/elements/WorkScheduleForm'
import { useEmployeeSchedule } from '@/hooks/useEmployeeSchedule'
import { useReplaceEmployeeSchedule } from '@/hooks/useReplaceEmployeeSchedule'
import type { Employee } from '@/services/employees'
import type { WorkingHoursInterval } from '@/services/schedules'

type EmployeeScheduleEditorProps = {
  employee: Employee
  onSaved: () => void
  onCancel: () => void
}

export function EmployeeScheduleEditor({
  employee,
  onSaved,
  onCancel,
}: EmployeeScheduleEditorProps) {
  const {
    data: schedule,
    error,
    isSuccess,
    isFetchedAfterMount,
    refetch,
  } = useEmployeeSchedule(employee.id)
  const replaceMutation = useReplaceEmployeeSchedule()
  const [reloads, setReloads] = useState(0)

  const save = async (intervals: WorkingHoursInterval[]) => {
    await replaceMutation.mutateAsync({ employeeId: employee.id, intervals })
    onSaved()
  }

  const reload = async () => {
    const result = await refetch()
    if (result.isSuccess) {
      setReloads((count) => count + 1)
    }
  }

  if (error) {
    return (
      <p
        role="alert"
        className="rounded-lg bg-destructive/10 px-3.5 py-3 text-sm font-medium text-destructive"
      >
        {error instanceof ApiError && error.status === 404
          ? 'This employee no longer exists.'
          : "We couldn't load the working hours. Please refresh the page."}
      </p>
    )
  }

  if (isSuccess && isFetchedAfterMount) {
    return (
      <WorkScheduleForm
        key={reloads}
        schedule={schedule}
        onSubmit={save}
        onCancel={onCancel}
        onReload={() => void reload()}
      />
    )
  }

  return <p className="text-muted-foreground">Loading…</p>
}
