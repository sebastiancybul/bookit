import { useQuery, type QueryClient } from '@tanstack/react-query'
import { businessOfferingsQueryKey } from '@/hooks/useBusinessOfferings'
import { listMyOfferings } from '@/services/offerings'

export const myOfferingsQueryKey = ['owner', 'offerings'] as const

export function refreshOfferings(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: myOfferingsQueryKey }),
    queryClient.invalidateQueries({ queryKey: businessOfferingsQueryKey }),
  ])
}

export function useMyOfferings() {
  return useQuery({
    queryKey: myOfferingsQueryKey,
    queryFn: listMyOfferings,
  })
}
