import { useQuery, type QueryClient } from '@tanstack/react-query'
import { businessEmployeesQueryKey } from '@/hooks/useBusinessEmployees'
import { listMyEmployees } from '@/services/employees'

export const myEmployeesQueryKey = ['owner', 'employees'] as const

export function refreshEmployees(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: myEmployeesQueryKey }),
    queryClient.invalidateQueries({ queryKey: businessEmployeesQueryKey }),
  ])
}

export function useMyEmployees() {
  return useQuery({
    queryKey: myEmployeesQueryKey,
    queryFn: listMyEmployees,
  })
}
