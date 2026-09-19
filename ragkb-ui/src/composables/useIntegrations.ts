// composables/useIntegrations.ts
import { ref, computed, toValue, type MaybeRefOrGetter } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { integrationsService } from '@/services'
import type { CreateIntegrationRequest, UpdateIntegrationRequest } from '@/types'

const INTEGRATIONS_KEY = ['integrations']

function extractMessage(error: unknown): string | undefined {
  return (error as { response?: { data?: { message?: string } } })?.response?.data?.message
}

export function useIntegrations() {
  const query = useQuery({
    queryKey: INTEGRATIONS_KEY,
    queryFn: integrationsService.list,
  })

  return {
    integrations: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useCreateIntegration() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const { mutateAsync, isPending: creating } = useMutation({
    mutationFn: (request: CreateIntegrationRequest) => integrationsService.create(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: INTEGRATIONS_KEY }),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível criar a integração.'
    },
  })

  return { createIntegration: mutateAsync, creating, errorMessage }
}

export function useUpdateIntegration() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const { mutateAsync, isPending: updating } = useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateIntegrationRequest }) =>
      integrationsService.update(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: INTEGRATIONS_KEY }),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível atualizar a integração.'
    },
  })

  return { updateIntegration: mutateAsync, updating, errorMessage }
}

export function useDeleteIntegration() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const { mutateAsync, isPending: deleting } = useMutation({
    mutationFn: (id: string) => integrationsService.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: INTEGRATIONS_KEY }),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível excluir a integração.'
    },
  })

  return { deleteIntegration: mutateAsync, deleting, errorMessage }
}

export function useIntegrationExecutions(id: MaybeRefOrGetter<string>) {
  const integrationId = computed(() => toValue(id))

  const query = useQuery({
    queryKey: ['integration-executions', integrationId],
    queryFn: () => integrationsService.executions(integrationId.value),
    // NUNCA executa com id vazio — evita GET /integrations//executions,
    // que o backend rejeita com 401 e o interceptor interpreta como
    // sessão expirada, apagando o localStorage e redirecionando ao login.
    enabled: computed(() => integrationId.value !== ''),
  })

  return {
    executions: query.data,
    isLoadingExecutions: query.isLoading,
    isErrorExecutions: query.isError,
    refetchExecutions: query.refetch,
  }
}
