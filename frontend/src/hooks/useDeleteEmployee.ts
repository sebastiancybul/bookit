import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refreshEmployees } from '@/hooks/useMyEmployees'
import { deleteEmployee } from '@/services/employees'

export function useDeleteEmployee() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteEmployee,
    onSuccess: () => refreshEmployees(queryClient),
  })
}
