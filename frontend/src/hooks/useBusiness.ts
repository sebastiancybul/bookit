import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/api/client'
import { getBusiness } from '@/services/businesses'

const MAX_RETRIES = 2
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
    retry: (failureCount, error) => {
      if (error instanceof ApiError && error.status < 500) {
        return false
      }
      return failureCount < MAX_RETRIES
    },
  })
}
