// ragkb-ui/src/composables/useConflicts.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { conflictsService } from '@/services'
import { queryKeys } from './queryKeys'
import type { ConflictActionRequest } from '@/types'

// ── QUERY: listar conflitos ──────────────────────────────
export function useConflicts() {
  const query = useQuery({
    queryKey: queryKeys.conflicts.lists(),
    queryFn: () => conflictsService.list(),
  })

  return {
    conflicts: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
  }
}

// ── MUTATION: escanear conflitos ──────────────────────────
export function useScanConflicts() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => conflictsService.scan(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.conflicts.lists() })
    },
  })

  return {
    scanConflicts: mutation.mutateAsync,
    scanning: mutation.isPending,
  }
}

// ── MUTATION: atualizar status do conflito ────────────────
export function useUpdateConflict() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: ({ id, action, note }: { id: number; action: ConflictActionRequest['action']; note?: string }) =>
      conflictsService.update(id, action, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.conflicts.lists() })
    },
  })

  return {
    updateConflict: mutation.mutateAsync,
    updating: mutation.isPending,
  }
}
