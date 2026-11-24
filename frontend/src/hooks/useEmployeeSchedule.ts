import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, retryUnlessClientError } from '@/api/client'
import { myEmployeesQueryKey } from '@/hooks/useMyEmployees'
import { getEmployeeSchedule } from '@/services/schedules'

export function employeeScheduleQueryKey(employeeId: string) {
  return [...myEmployeesQueryKey, employeeId, 'schedule'] as const
}

export function useEmployeeSchedule(employeeId: string) {
  const queryClient = useQueryClient()

  return useQuery({
    queryKey: employeeScheduleQueryKey(employeeId),
    queryFn: async () => {
      try {
        return await getEmployeeSchedule(employeeId)
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          void queryClient.invalidateQueries({
            queryKey: myEmployeesQueryKey,
            exact: true,
          })
        }
        throw error
      }
    },
    retry: retryUnlessClientError,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
}
