import { useMutation, useQueryClient } from '@tanstack/react-query'
import { myBusinessQueryKey } from '@/hooks/useMyBusiness'
import { updateMyBusiness } from '@/services/businesses'

export function useUpdateMyBusiness() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateMyBusiness,
    onSuccess: (business) => {
      queryClient.setQueryData(myBusinessQueryKey, business)
    },
  })
}
