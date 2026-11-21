import { useMutation, useQueryClient } from '@tanstack/react-query'
import { setSessionUser } from '@/hooks/useMe'
import { logout } from '@/services/auth'

export function useLogout() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      setSessionUser(queryClient, null)
    },
  })
}
