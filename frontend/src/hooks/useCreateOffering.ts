import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refreshOfferings } from '@/hooks/useMyOfferings'
import { createOffering } from '@/services/offerings'

export function useCreateOffering() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createOffering,
    onSuccess: () => refreshOfferings(queryClient),
  })
}
