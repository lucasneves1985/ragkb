import { ref, computed, type Ref } from 'vue'
import { useMutation } from '@tanstack/vue-query'
import { queryService, conversationsService } from '@/services'
import { useAuthStore } from '@/stores/auth'
import type { Message, AskRequest, ChatMessageItem, SourceReference } from '@/types'

export function useChat({ messagesContainer }: { messagesContainer?: Ref<HTMLElement | null> } = {}) {
  const auth = useAuthStore()
  const question = ref('')
  const messages = ref<Message[]>([])
  const currentConversationId = ref<string | undefined>()
  const loadingHistory = ref(false)
  const ticket = ref({ subject: '', description: '' })

  const askMutation = useMutation({
    mutationFn: (payload: AskRequest | string) => {
      const body: AskRequest =
        typeof payload === 'string'
          ? { question: payload, conversationId: currentConversationId.value }
          : payload
      return queryService.ask(body, currentConversationId.value)
    },
    onMutate: async (_payload) => {
      const userMsg: Message = {
        from: 'user',
        text: question.value,
        timestamp: new Date().toISOString(),
      }
      messages.value.push(userMsg)
      question.value = ''
    },
    onSuccess: (response) => {
      const fallbackSources: SourceReference[] = (response.sourceIds ?? []).map((label) => ({
        type: 'DOCUMENT',
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

  const loading = computed(() => askMutation.isPending.value || loadingHistory.value)
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
    callback?.()
  }

  async function selectConversation(id: string) {
    if (currentConversationId.value === id) return
    currentConversationId.value = id
    loadingHistory.value = true
    ticket.value = { subject: '', description: '' }
    try {
      const detail = await conversationsService.get(id)
      messages.value = detail.messages.map(mapHistoryMessage)
    } catch {
      messages.value = [
        {
          from: 'assistant',
          text: 'Não foi possível carregar as mensagens da conversa.',
          timestamp: new Date().toISOString(),
        },
      ]
    } finally {
      loadingHistory.value = false
    }
    scrollToBottom()
  }

  function mapHistoryMessage(m: ChatMessageItem): Message {
    if (m.sender === 'USER') {
      return { id: m.id, from: 'user', text: m.content, timestamp: m.createdAt }
    }
    if (m.status === 'TICKET_SUGGESTED' && m.ticketSuggestion) {
      ticket.value = {
        subject: m.ticketSuggestion.subject,
        description: m.ticketSuggestion.description,
      }
      return { id: m.id, from: 'assistant', ticket: true, timestamp: m.createdAt }
    }
    // Prioriza fontes estruturadas (com link para artigo). Fallback:
    // labels legados mapeados como DOCUMENT — o ChatMessageList já
    // trata os dois formatos via union string | SourceReference
    const sources: (string | SourceReference)[] =
      m.structuredSources && m.structuredSources.length > 0
        ? m.structuredSources
        : (m.sources ?? [])
    return {
      id: m.id,
      from: 'assistant',
      text: m.content,
      sources,
      timestamp: m.createdAt,
    }
  }

  function startNewConversation() {
    currentConversationId.value = undefined
    messages.value = []
    ticket.value = { subject: '', description: '' }
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
