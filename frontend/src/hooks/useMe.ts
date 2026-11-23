import { hashKey, useQuery, type QueryClient } from '@tanstack/react-query'
import { retryUnlessClientError } from '@/api/client'
import { getMe, type User } from '@/services/auth'

export const meQueryKey = ['auth', 'me'] as const

export function setSessionUser(queryClient: QueryClient, user: User | null) {
  queryClient.removeQueries({
    predicate: (query) => query.queryHash !== hashKey(meQueryKey),
  })
  queryClient.setQueryData(meQueryKey, user)
}

export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: getMe,
    staleTime: 5 * 60 * 1000,
    retry: retryUnlessClientError,
  })
}
