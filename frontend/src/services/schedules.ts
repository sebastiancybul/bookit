import { client, unwrap } from '@/api/client'
import type { components } from '@/api/schema'

export type WorkingHoursInterval =
  components['schemas']['WorkingHoursIntervalDto']
export type WorkSchedule = components['schemas']['WorkScheduleResponse']

export async function getEmployeeSchedule(
  employeeId: string,
): Promise<WorkSchedule> {
  return unwrap(
    await client.GET('/api/v1/owner/business/employees/{employeeId}/schedule', {
      params: { path: { employeeId } },
    }),
  )
}

export async function replaceEmployeeSchedule({
  employeeId,
  intervals,
}: {
  employeeId: string
  intervals: WorkingHoursInterval[]
}): Promise<WorkSchedule> {
  return unwrap(
    await client.PUT('/api/v1/owner/business/employees/{employeeId}/schedule', {
      params: { path: { employeeId } },
      body: { intervals },
    }),
  )
}
