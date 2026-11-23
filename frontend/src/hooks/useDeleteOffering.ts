import { useMutation, useQueryClient } from '@tanstack/react-query'
import { refreshOfferings } from '@/hooks/useMyOfferings'
import { deleteOffering } from '@/services/offerings'

export function useDeleteOffering() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteOffering,
    onSuccess: () => refreshOfferings(queryClient),
  })
}
