import { useQuery } from '@tanstack/react-query'
import { listCategories } from '@/services/businesses'

export const categoriesQueryKey = ['categories'] as const

export function useCategories() {
  return useQuery({
    queryKey: categoriesQueryKey,
    queryFn: listCategories,
    staleTime: Infinity,
  })
}
