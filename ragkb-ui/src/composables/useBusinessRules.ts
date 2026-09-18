// composables/useBusinessRules.ts
import { ref } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { businessRulesService } from '@/services'
import type { CreateBusinessRuleRequest, UpdateBusinessRuleRequest } from '@/types'

const RULES_KEY = ['business-rules']

function extractMessage(error: unknown): string | undefined {
  return (error as { response?: { data?: { message?: string } } })?.response?.data?.message
}

// Padrão do projeto (useSectors/useArticles): retorna objeto próprio,
// nunca o UseQueryReturnType cru — mantém a convenção única entre composables.
export function useBusinessRules() {
  const query = useQuery({
    queryKey: RULES_KEY,
    queryFn: businessRulesService.list,
  })

  return {
    rules: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useCreateBusinessRule() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const { mutateAsync, isPending: creating } = useMutation({
    mutationFn: (request: CreateBusinessRuleRequest) => businessRulesService.create(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RULES_KEY }),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível criar a regra.'
    },
  })

  return { createBusinessRule: mutateAsync, creating, errorMessage }
}

export function useUpdateBusinessRule() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const { mutateAsync, isPending: updating } = useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateBusinessRuleRequest }) =>
      businessRulesService.update(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RULES_KEY }),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível atualizar a regra.'
    },
  })

  return { updateBusinessRule: mutateAsync, updating, errorMessage }
}

export function usePublishBusinessRule() {
  const queryClient = useQueryClient()

  const { mutateAsync, isPending: publishing } = useMutation({
    mutationFn: (id: string) => businessRulesService.publish(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RULES_KEY }),
  })

  return { publishBusinessRule: mutateAsync, publishing }
}

export function useArchiveBusinessRule() {
  const queryClient = useQueryClient()

  const { mutateAsync, isPending: archiving } = useMutation({
    mutationFn: (id: string) => businessRulesService.archive(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: RULES_KEY }),
  })

  return { archiveBusinessRule: mutateAsync, archiving }
}

export function usePortalBusinessRules() {
  const query = useQuery({
    queryKey: ['business-rules-portal'],
    queryFn: () => businessRulesService.portal(),
  })

  return {
    portalRules: query.data,
    isLoadingPortal: query.isLoading,
    isErrorPortal: query.isError,
    refetchPortal: query.refetch,
  }
}
