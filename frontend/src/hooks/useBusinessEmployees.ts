import { useQuery } from '@tanstack/react-query'
import { retryUnlessClientError } from '@/api/client'
import { businessesQueryKey } from '@/hooks/useBusiness'
import { listBusinessEmployees } from '@/services/employees'

const FRESH_FOR_MS = 60 * 1000

export const businessEmployeesQueryKey = [
  ...businessesQueryKey,
  'employees',
] as const

export function useBusinessEmployees(slug: string) {
  return useQuery({
    queryKey: [...businessEmployeesQueryKey, slug],
    queryFn: () => listBusinessEmployees(slug),
    staleTime: FRESH_FOR_MS,
    retry: retryUnlessClientError,
  })
}
