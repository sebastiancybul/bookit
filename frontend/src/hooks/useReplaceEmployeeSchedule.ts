import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/api/client'
import { employeeScheduleQueryKey } from '@/hooks/useEmployeeSchedule'
import { myEmployeesQueryKey } from '@/hooks/useMyEmployees'
import { replaceEmployeeSchedule } from '@/services/schedules'

export function useReplaceEmployeeSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: replaceEmployeeSchedule,
    onSuccess: (schedule, { employeeId }) => {
      queryClient.setQueryData(employeeScheduleQueryKey(employeeId), schedule)
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 404) {
        return queryClient.invalidateQueries({
          queryKey: myEmployeesQueryKey,
          exact: true,
        })
      }
    },
  })
}
