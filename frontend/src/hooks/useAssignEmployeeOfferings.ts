import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refreshEmployees } from '@/hooks/useMyEmployees'
import { assignEmployeeOfferings } from '@/services/employees'

export function useAssignEmployeeOfferings() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: assignEmployeeOfferings,
    onSuccess: () => refreshEmployees(queryClient),
  })
}
