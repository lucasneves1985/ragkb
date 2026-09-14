// ragkb-ui/src/composables/useConversations.ts
import { computed } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { conversationsService } from '@/services'
import { queryKeys } from './queryKeys'
import type { Conversation } from '@/types'

// ── QUERY: listar conversas ───────────────────────────────
export function useConversations() {
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: queryKeys.conversations.lists(),
    queryFn: () => conversationsService.list(),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => conversationsService.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.conversations.lists() })
    },
  })

  const conversations = computed<Conversation[]>(() => query.data.value ?? [])

  async function loadConversations() {
    await query.refetch()
  }

  async function deleteConversation(id: string): Promise<boolean> {
    try {
      await deleteMutation.mutateAsync(id)
      return true
    } catch {
      return false
    }
  }

  return {
    conversations,
    loadConversations,
    deleteConversation,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
  }
}

// ── MUTATION: deletar conversa ────────────────────────────
export function useDeleteConversation() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (id: string) => conversationsService.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.conversations.lists() })
    },
  })

  return {
    deleteConversation: mutation.mutateAsync,
    deleting: mutation.isPending,
  }
}

