import { useQuery } from '@tanstack/react-query'
import { ApiError } from '@/api/client'
import { getMe } from '@/services/auth'

export const meQueryKey = ['auth', 'me'] as const

const MAX_RETRIES = 2

export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: getMe,
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error) => {
      if (error instanceof ApiError && error.status < 500) {
        return false
      }
      return failureCount < MAX_RETRIES
    },
  })
}
