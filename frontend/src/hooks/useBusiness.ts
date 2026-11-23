import { useQuery, useQueryClient } from '@tanstack/react-query'
import { retryUnlessClientError } from '@/api/client'
import { getBusiness } from '@/services/businesses'

const FRESH_FOR_MS = 60 * 1000

export const businessesQueryKey = ['businesses'] as const

export function businessQueryKey(slug: string) {
  return [...businessesQueryKey, 'detail', slug] as const
}

export function useBusiness(slug: string) {
  const queryClient = useQueryClient()

  return useQuery({
    queryKey: businessQueryKey(slug),
    queryFn: async () => {
      const business = await getBusiness(slug)
      if (business.slug !== slug) {
        queryClient.setQueryData(businessQueryKey(business.slug), business)
      }
      return business
    },
    staleTime: FRESH_FOR_MS,
    retry: retryUnlessClientError,
  })
}
