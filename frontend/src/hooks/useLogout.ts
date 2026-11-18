import { hashKey, useMutation, useQueryClient } from '@tanstack/react-query'
import { meQueryKey } from '@/hooks/useMe'
import { logout } from '@/services/auth'

export function useLogout() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(meQueryKey, null)
      queryClient.removeQueries({
        predicate: (query) => query.queryHash !== hashKey(meQueryKey),
      })
    },
  })
}
