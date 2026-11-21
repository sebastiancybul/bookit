import { useMutation, useQueryClient } from '@tanstack/react-query'
import { meQueryKey } from '@/hooks/useMe'
import { myBusinessQueryKey } from '@/hooks/useMyBusiness'
import type { User } from '@/services/auth'
import { createBusiness } from '@/services/businesses'

export function useCreateBusiness() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createBusiness,
    onSuccess: (business) => {
      queryClient.setQueryData(myBusinessQueryKey, business)
      queryClient.setQueryData<User | null>(meQueryKey, (user) =>
        user && !user.roles.includes('ROLE_OWNER')
          ? { ...user, roles: [...user.roles, 'ROLE_OWNER'] }
          : user,
      )
    },
  })
}
