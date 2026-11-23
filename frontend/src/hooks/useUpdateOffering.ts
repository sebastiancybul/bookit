import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refreshOfferings } from '@/hooks/useMyOfferings'
import { updateOffering } from '@/services/offerings'

export function useUpdateOffering() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateOffering,
    onSuccess: () => refreshOfferings(queryClient),
  })
}
