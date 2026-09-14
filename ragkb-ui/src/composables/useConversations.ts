import { ref } from 'vue'

import { conversationsService } from '@/services'
import type { Conversation } from '@/types'

export function useConversations() {
  const conversations = ref<Conversation[]>([])
  const loading = ref(false)
  const deleting = ref(false)
  const errorMessage = ref('')

  async function loadConversations(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    try {
      conversations.value = await conversationsService.list()
    } catch {
      errorMessage.value = 'Não foi possível carregar as conversas.'
    } finally {
      loading.value = false
    }
  }

  async function deleteConversation(id: string): Promise<boolean> {
    deleting.value = true
    errorMessage.value = ''

    try {
      await conversationsService.remove(id)

      conversations.value = conversations.value.filter((conversation) => conversation.id !== id)

      return true
    } catch {
      errorMessage.value = 'Não foi possível excluir a conversa.'
      return false
    } finally {
      deleting.value = false
    }
  }

  return {
    conversations,
    loading,
    deleting,
    errorMessage,
    loadConversations,
    deleteConversation,
  }
}
