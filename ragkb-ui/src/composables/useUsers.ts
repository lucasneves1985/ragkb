import { ref } from 'vue'
import { usersService } from '@/services'
import type { CreateUserRequest, User } from '@/types'

export function useUsers() {
  const users = ref<User[]>([])
  const loading = ref(false)
  const submitting = ref(false)
  const errorMessage = ref('')

  async function loadUsers(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    try {
      users.value = await usersService.list()
    } catch {
      errorMessage.value = 'Não foi possível carregar os usuários.'
    } finally {
      loading.value = false
    }
  }

  async function createUser(
    usernameOrRequest: string | CreateUserRequest,
    password?: string,
    roles?: string[],
    sectorId?: number,
  ): Promise<User | null> {
    submitting.value = true
    errorMessage.value = ''

    try {
      const user = await usersService.create(usernameOrRequest, password, roles, sectorId)
      await loadUsers()
      return user
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } }).response?.status
      errorMessage.value =
        status === 409
          ? 'Este nome de usuário já está em uso.'
          : 'Não foi possível cadastrar o usuário.'
      return null
    } finally {
      submitting.value = false
    }
  }

  function clearError(): void {
    errorMessage.value = ''
  }

  return {
    users,
    loading,
    submitting,
    errorMessage,
    loadUsers,
    createUser,
    clearError,
  }
}
