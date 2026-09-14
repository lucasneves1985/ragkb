import { computed, nextTick, reactive, ref } from 'vue'
import { conversationsService, queryService, ticketsService } from '@/services'
import type { ChatMessageItem } from '@/types'
import { useAuthStore } from '../stores/auth'

export type Message = {
  id: number
  from: 'user' | 'assistant'
  text?: string
  sources?: string[]
  ticket?: boolean
  ticketSent?: boolean
}

export function useChat(options?: { messagesContainer?: { value: HTMLElement | null } }) {
  const auth = useAuthStore()
  const question = ref('')
  const loading = ref(false)
  const errorMessage = ref('')
  const currentConversationId = ref<string | null>(null)

  async function scrollToBottom() {
    await nextTick()
    if (options?.messagesContainer?.value) {
      options.messagesContainer.value.scrollTop = options.messagesContainer.value.scrollHeight
    }
  }

  const defaultGreeting: Message = {
    id: 1,
    from: 'assistant',
    text: 'Olá, **' + auth.username + '**. Posso ajudar a localizar informações na base corporativa. O que você precisa saber?',
    sources: [],
  }

  const messages = ref<Message[]>([defaultGreeting])

  const ticket = reactive({
    subject: 'Assunto',
    description: 'Descrição detalhada do assunto.',
    requester: auth.username,
  })

  const canSend = computed(() => question.value.trim().length > 0 && !loading.value)

  async function selectConversation(id: string) {
    if (currentConversationId.value === id) return
    currentConversationId.value = id
    loading.value = true
    errorMessage.value = ''
    try {
      const data = await conversationsService.get(id)
      messages.value = data.messages.map((m: ChatMessageItem) => {
        if (m.sender === 'USER') {
          return { id: m.id, from: 'user', text: m.content }
        } else {
          if (m.status === 'TICKET_SUGGESTED' && m.ticketSuggestion) {
            Object.assign(ticket, m.ticketSuggestion)
            return { id: m.id, from: 'assistant', ticket: true }
          }
          return { id: m.id, from: 'assistant', text: m.content, sources: m.sources }
        }
      })
      if (messages.value.length === 0) {
        messages.value = [defaultGreeting]
      }
      scrollToBottom()
    } catch {
      errorMessage.value = 'Erro ao carregar mensagens da conversa.'
    } finally {
      loading.value = false
    }
  }

  function startNewConversation() {
    currentConversationId.value = null
    messages.value = [{ ...defaultGreeting }]
    scrollToBottom()
  }

  async function ask(onSuccess?: () => void) {
    if (!canSend.value) return
    const q = question.value.trim()
    messages.value.push({ id: Date.now(), from: 'user', text: q })
    question.value = ''
    loading.value = true
    errorMessage.value = ''
    scrollToBottom()

    try {
      const data = await queryService.ask(q, currentConversationId.value || undefined)

      if (data.conversationId) {
        currentConversationId.value = data.conversationId
      }

      if (data.status === 'TICKET_SUGGESTED') {
        if (data.ticketSuggestion) {
          Object.assign(ticket, data.ticketSuggestion)
        }
        messages.value.push({ id: Date.now() + 1, from: 'assistant', ticket: true })
      } else {
        messages.value.push({
          id: Date.now() + 1,
          from: 'assistant',
          text: data.answer,
          sources: data.sourceIds,
        })
      }

      if (onSuccess) {
        onSuccess()
      }
    } catch {
      errorMessage.value = 'Não foi possível consultar a base de conhecimento no momento.'
      messages.value.push({
        id: Date.now() + 1,
        from: 'assistant',
        text: 'Não foi possível consultar a base de conhecimento no momento.',
      })
    } finally {
      loading.value = false
      scrollToBottom()
    }
  }

  async function openTicket(message: Message) {
    try {
      await ticketsService.create(ticket)
      message.ticketSent = true
    } catch {
      message.ticketSent = false
      errorMessage.value = 'Não foi possível abrir o chamado técnico.'
    }
  }

  function clearError() {
    errorMessage.value = ''
  }

  return {
    auth,
    question,
    loading,
    messages,
    ticket,
    currentConversationId,
    canSend,
    errorMessage,
    scrollToBottom,
    selectConversation,
    startNewConversation,
    ask,
    openTicket,
    clearError,
  }
}
