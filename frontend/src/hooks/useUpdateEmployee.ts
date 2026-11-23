import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refreshEmployees } from '@/hooks/useMyEmployees'
import { updateEmployee } from '@/services/employees'

export function useUpdateEmployee() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateEmployee,
    onSuccess: () => refreshEmployees(queryClient),
  })
}
