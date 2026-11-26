import { useQuery } from '@tanstack/react-query'
import { retryUnlessClientError } from '@/api/client'
import { businessesQueryKey } from '@/hooks/useBusiness'
import {
  getAvailableSlots,
  type AvailabilityQuery,
} from '@/services/availability'

const FRESH_FOR_MS = 30 * 1000

export const availabilityQueryKey = [
  ...businessesQueryKey,
  'availability',
] as const

export function useAvailableSlots(query: AvailabilityQuery) {
  return useQuery({
    queryKey: [
      ...availabilityQueryKey,
      query.slug,
      query.offeringId,
      query.employeeId,
      query.date,
    ],
    queryFn: () => getAvailableSlots(query),
    staleTime: FRESH_FOR_MS,
    retry: retryUnlessClientError,
  })
}
