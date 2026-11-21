import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ApiError } from '@/api/client'
import { businessesQueryKey } from '@/hooks/useBusiness'
import { searchBusinesses, type BusinessSearch } from '@/services/businesses'

const MAX_RETRIES = 2

export function useBusinessSearch(search: BusinessSearch) {
  return useQuery({
    queryKey: [...businessesQueryKey, 'search', search],
    queryFn: () => searchBusinesses(search),
    placeholderData: keepPreviousData,
    retry: (failureCount, error) => {
      if (error instanceof ApiError && error.status < 500) {
        return false
      }
      return failureCount < MAX_RETRIES
    },
  })
}
