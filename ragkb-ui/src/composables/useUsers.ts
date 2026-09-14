import { ref, computed } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { usersService } from '@/services'
import { queryKeys } from './queryKeys'
import type { CreateUserRequest, User } from '@/types'

export function useUsers() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const query = useQuery({
    queryKey: queryKeys.users.lists(),
    queryFn: () => usersService.list(),
  })

  const createMutation = useMutation({
    mutationFn: ({
      usernameOrRequest,
      password,
      roles,
      sectorId,
    }: {
      usernameOrRequest: string | CreateUserRequest
      password?: string
      roles?: string[]
      sectorId?: number
    }) => usersService.create(usernameOrRequest, password, roles, sectorId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users.lists() })
    },
  })

  const users = computed<User[]>(() => query.data.value ?? [])
  const loading = computed(() => query.isLoading.value)
  const submitting = computed(() => createMutation.isPending.value)

  async function loadUsers(): Promise<void> {
    errorMessage.value = ''
    try {
      await query.refetch()
    } catch {
      errorMessage.value = 'Não foi possível carregar os usuários.'
    }
  }

  async function createUser(
    usernameOrRequest: string | CreateUserRequest,
    password?: string,
    roles?: string[],
    sectorId?: number,
  ): Promise<User | null> {
    errorMessage.value = ''
    try {
      return await createMutation.mutateAsync({
        usernameOrRequest,
        password,
        roles,
        sectorId,
      })
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } }).response?.status
      errorMessage.value =
        status === 409
          ? 'Este nome de usuário já está em uso.'
          : 'Não foi possível cadastrar o usuário.'
      return null
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
    refetch: query.refetch,
  }
}

