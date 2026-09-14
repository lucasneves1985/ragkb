import { ref } from 'vue'
import { conflictsService } from '@/services'
import type { Conflict, ConflictActionRequest } from '@/types'

export function useConflicts() {
  const conflicts = ref<Conflict[]>([])
  const loading = ref(false)
  const scanning = ref(false)
  const updating = ref(false)
  const errorMessage = ref('')

  async function loadConflicts(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    try {
      conflicts.value = await conflictsService.list()
    } catch {
      errorMessage.value = 'Não foi possível carregar os conflitos.'
    } finally {
      loading.value = false
    }
  }

  async function scanConflicts(): Promise<void> {
    scanning.value = true
    errorMessage.value = ''

    try {
      conflicts.value = await conflictsService.scan()
    } catch {
      errorMessage.value = 'Não foi possível executar a varredura de conflitos.'
    } finally {
      scanning.value = false
    }
  }

  async function updateConflict(
    id: number,
    actionOrRequest: 'DISMISSED' | 'REVIEWED' | 'RESOLVED' | ConflictActionRequest,
    note?: string,
  ): Promise<Conflict | null> {
    updating.value = true
    errorMessage.value = ''

    try {
      const updatedConflict = await conflictsService.update(id, actionOrRequest, note)
      await loadConflicts()
      return updatedConflict
    } catch {
      errorMessage.value = 'Não foi possível atualizar o conflito.'
      return null
    } finally {
      updating.value = false
    }
  }

  function clearError(): void {
    errorMessage.value = ''
  }

  return {
    conflicts,
    loading,
    scanning,
    updating,
    errorMessage,
    loadConflicts,
    scanConflicts,
    updateConflict,
    clearError,
  }
}
