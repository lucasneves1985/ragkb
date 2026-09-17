import { ref, computed } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { usersService } from '@/services'
import { queryKeys } from './queryKeys'
import type { CreateUserRequest, UpdateUserRequest, User } from '@/types'

export function useUsers() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const query = useQuery({
    queryKey: queryKeys.users.lists(),
    queryFn: () => usersService.list(),
  })

  const createMutation = useMutation({
    mutationFn: (payload: CreateUserRequest) => usersService.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users.lists() })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateUserRequest }) =>
      usersService.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.users.lists() })
    },
  })

  const users = computed<User[]>(() => query.data.value ?? [])
  const loading = computed(() => query.isLoading.value)
  const submitting = computed(
    () => createMutation.isPending.value || updateMutation.isPending.value,
  )

  async function loadUsers(): Promise<void> {
    errorMessage.value = ''
    try {
      await query.refetch()
    } catch {
      errorMessage.value = 'Não foi possível carregar os usuários.'
    }
  }

  async function createUser(payload: CreateUserRequest): Promise<User | null> {
    errorMessage.value = ''
    try {
      return await createMutation.mutateAsync(payload)
    } catch (e: unknown) {
      errorMessage.value = resolveError(e, 'Não foi possível cadastrar o usuário.')
      return null
    }
  }

  async function updateUser(
    id: number,
    payload: UpdateUserRequest,
  ): Promise<User | null> {
    errorMessage.value = ''
    try {
      return await updateMutation.mutateAsync({ id, payload })
    } catch (e: unknown) {
      errorMessage.value = resolveError(e, 'Não foi possível salvar as alterações do usuário.')
      return null
    }
  }

  function resolveError(e: unknown, fallback: string): string {
    const status = (e as { response?: { status?: number } }).response?.status
    if (status === 409) return 'Nome de usuário ou e-mail já está em uso.'
    if (status === 404) return 'Usuário não encontrado.'
    return fallback
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
    updateUser,
    clearError,
    refetch: query.refetch,
  }
}
