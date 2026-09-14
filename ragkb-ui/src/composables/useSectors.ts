// ragkb-ui/src/composables/useSectors.ts
import { ref, computed } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { sectorsService } from '@/services'
import { queryKeys } from './queryKeys'
import type { Sector } from '@/types'

export function useSectors() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const query = useQuery({
    queryKey: queryKeys.sectors.lists(),
    queryFn: () => sectorsService.list(),
  })

  const createMutation = useMutation({
    mutationFn: (name: string) => sectorsService.create(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sectors.lists() })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => sectorsService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sectors.lists() })
    },
  })

  const sectors = computed<Sector[]>(() => query.data.value ?? [])
  const saving = computed(() => createMutation.isPending.value || deleteMutation.isPending.value)

  async function loadSectors() {
    errorMessage.value = ''
    try {
      await query.refetch()
    } catch {
      errorMessage.value = 'Não foi possível carregar os setores.'
    }
  }

  async function createSector(name: string): Promise<Sector | null> {
    errorMessage.value = ''
    try {
      return await createMutation.mutateAsync(name)
    } catch {
      errorMessage.value = 'Não foi possível cadastrar o setor.'
      return null
    }
  }

  async function deleteSector(id: number): Promise<boolean> {
    errorMessage.value = ''
    try {
      await deleteMutation.mutateAsync(id)
      return true
    } catch {
      errorMessage.value = 'Não foi possível excluir o setor.'
      return false
    }
  }

  return {
    sectors,
    saving,
    errorMessage,
    loadSectors,
    createSector,
    deleteSector,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
  }
}

