import { useMutation, useQueryClient } from '@tanstack/react-query'
import { setSessionUser } from '@/hooks/useMe'
import { login } from '@/services/auth'

export function useLogin() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: login,
    onSuccess: (user) => {
      setSessionUser(queryClient, user)
    },
  })
}
