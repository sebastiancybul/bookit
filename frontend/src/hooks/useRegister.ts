import { useMutation, useQueryClient } from '@tanstack/react-query'
import { setSessionUser } from '@/hooks/useMe'
import { register } from '@/services/auth'

export function useRegister() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: register,
    onSuccess: (user) => {
      setSessionUser(queryClient, user)
    },
  })
}
