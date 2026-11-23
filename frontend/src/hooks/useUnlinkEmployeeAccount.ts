import { useMutation, useQueryClient } from '@tanstack/react-query'
import { meQueryKey } from '@/hooks/useMe'
import { refreshEmployees } from '@/hooks/useMyEmployees'
import { unlinkEmployeeAccount } from '@/services/employees'

export function useUnlinkEmployeeAccount() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: unlinkEmployeeAccount,
    onSuccess: () =>
      Promise.all([
        refreshEmployees(queryClient),
        queryClient.invalidateQueries({ queryKey: meQueryKey }),
      ]),
  })
}
