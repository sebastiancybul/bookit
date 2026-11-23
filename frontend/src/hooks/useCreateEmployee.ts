import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refreshEmployees } from '@/hooks/useMyEmployees'
import { createEmployee } from '@/services/employees'

export function useCreateEmployee() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createEmployee,
    onSuccess: () => refreshEmployees(queryClient),
  })
}
