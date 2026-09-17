import { ref, computed, type Ref } from 'vue'
import { useMutation } from '@tanstack/vue-query'
import { queryService } from '@/services'
import { useAuthStore } from '@/stores/auth'
import type { Message, AskRequest } from '@/types'

export function useChat({ messagesContainer }: { messagesContainer?: Ref<HTMLElement | null> } = {}) {
  const auth = useAuthStore()
  const question = ref('')
  const messages = ref<Message[]>([])
  const currentConversationId = ref<string | undefined>()
  const ticket = ref({ subject: '', description: '' })

  // MUTATION: ask (POST /query) — não é query cacheável
  const askMutation = useMutation({
    mutationFn: (payload: AskRequest | string) => {
      const body: AskRequest =
        typeof payload === 'string'
          ? { question: payload, conversationId: currentConversationId.value }
          : payload
      return queryService.ask(body, currentConversationId.value)
    },
    onMutate: async (_payload) => {
      // adiciona mensagem do usuário imediatamente
      const userMsg: Message = {
        from: 'user',
        text: question.value,
        timestamp: new Date().toISOString(),
      }
      messages.value.push(userMsg)
      question.value = ''
    },
    onSuccess: (response) => {
      // Fontes estruturadas com fallback para backend legado (apenas labels)
      const fallbackSources = (response.sourceIds ?? []).map((label) => ({
        type: 'DOCUMENT' as const,
        label,
      }))
      messages.value.push({
        from: 'assistant',
        text: response.answer,
        sources: response.sources ?? fallbackSources,
        timestamp: new Date().toISOString(),
      })
      if (response.conversationId) {
        currentConversationId.value = response.conversationId
      }
      scrollToBottom()
    },
    onError: (error) => {
      messages.value.push({
        from: 'assistant',
        text: `Erro: ${error.message}`,
        timestamp: new Date().toISOString(),
      })
      scrollToBottom()
    },
  })

  const loading = computed(() => askMutation.isPending.value)
  const canSend = computed(() => question.value.trim().length > 0 && !loading.value)

  function scrollToBottom() {
    requestAnimationFrame(() => {
      if (messagesContainer?.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
  }

  async function ask(callback?: () => void) {
    if (!canSend.value) return
    await askMutation.mutateAsync(question.value)
    // invalida conversas após responder (nova mensagem na lista)
    callback?.()
  }

  function selectConversation(id: string) {
    currentConversationId.value = id
  }

  function startNewConversation() {
    currentConversationId.value = undefined
    messages.value = []
  }

  function openTicket(message: Message) {
    const text = message.text || message.content || ''
    ticket.value = {
      subject: `Erro na resposta: ${text.slice(0, 50)}`,
      description: text,
    }
  }

  return {
    auth,
    question,
    loading,
    messages,
    ticket,
    currentConversationId,
    canSend,
    scrollToBottom,
    selectConversation,
    startNewConversation,
    ask,
    openTicket,
  }
}
