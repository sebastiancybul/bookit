import { useMutation, useQueryClient } from '@tanstack/react-query'
import { meQueryKey } from '@/hooks/useMe'
import { refreshEmployees } from '@/hooks/useMyEmployees'
import { linkEmployeeAccount } from '@/services/employees'

export function useLinkEmployeeAccount() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: linkEmployeeAccount,
    onSuccess: () =>
      Promise.all([
        refreshEmployees(queryClient),
        queryClient.invalidateQueries({ queryKey: meQueryKey }),
      ]),
  })
}
