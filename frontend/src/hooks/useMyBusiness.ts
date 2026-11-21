import { useQuery } from '@tanstack/react-query'
import { getMyBusiness } from '@/services/businesses'

export const myBusinessQueryKey = ['owner', 'business'] as const

export function useMyBusiness() {
  return useQuery({
    queryKey: myBusinessQueryKey,
    queryFn: getMyBusiness,
  })
}
