import { ref } from 'vue'
import { sectorsService } from '@/services'
import type { Sector } from '@/types'

export function useSectors() {
  const sectors = ref<Sector[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')

  async function loadSectors(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    try {
      sectors.value = await sectorsService.list()
    } catch {
      errorMessage.value = 'Não foi possível carregar os setores.'
    } finally {
      loading.value = false
    }
  }

  async function createSector(name: string): Promise<Sector | null> {
    saving.value = true
    errorMessage.value = ''

    try {
      const sector = await sectorsService.create(name)
      await loadSectors()
      return sector
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } }).response?.status
      errorMessage.value =
        status === 409 ? 'Já existe um setor com esse nome.' : 'Erro ao cadastrar setor.'
      return null
    } finally {
      saving.value = false
    }
  }

  async function deleteSector(id: number): Promise<boolean> {
    errorMessage.value = ''

    try {
      await sectorsService.delete(id)
      await loadSectors()
      return true
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } }).response?.status
      errorMessage.value =
        status === 409
          ? 'Setor em uso. Reatribua usuários e documentos antes de excluir.'
          : 'Erro ao excluir setor.'
      return false
    }
  }

  function clearError(): void {
    errorMessage.value = ''
  }

  return {
    sectors,
    loading,
    saving,
    errorMessage,
    loadSectors,
    createSector,
    deleteSector,
    clearError,
  }
}
