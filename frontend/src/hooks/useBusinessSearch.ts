import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { retryUnlessClientError } from '@/api/client'
import { businessesQueryKey } from '@/hooks/useBusiness'
import { searchBusinesses, type BusinessSearch } from '@/services/businesses'

export function useBusinessSearch(search: BusinessSearch) {
  return useQuery({
    queryKey: [...businessesQueryKey, 'search', search],
    queryFn: () => searchBusinesses(search),
    placeholderData: keepPreviousData,
    retry: retryUnlessClientError,
  })
}
