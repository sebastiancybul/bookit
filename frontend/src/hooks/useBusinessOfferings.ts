import { useQuery } from '@tanstack/react-query'
import { retryUnlessClientError } from '@/api/client'
import { businessesQueryKey } from '@/hooks/useBusiness'
import { listBusinessOfferings } from '@/services/offerings'

const FRESH_FOR_MS = 60 * 1000

export const businessOfferingsQueryKey = [
  ...businessesQueryKey,
  'offerings',
] as const

export function useBusinessOfferings(slug: string) {
  return useQuery({
    queryKey: [...businessOfferingsQueryKey, slug],
    queryFn: () => listBusinessOfferings(slug),
    staleTime: FRESH_FOR_MS,
    retry: retryUnlessClientError,
  })
}
