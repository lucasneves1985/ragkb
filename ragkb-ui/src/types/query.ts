export interface AskRequest {
  question: string
  conversationId?: string
}

export interface TicketSuggestion {
  subject: string
  description: string
  requester: string
  category?: string
  tags?: string[]
}

export interface AnswerResponse {
  status: 'KNOWLEDGE' | 'TICKET_SUGGESTED'
  answer?: string
  sourceIds?: string[]
  ticketSuggestion?: TicketSuggestion
  conversationId?: string
}

export type ChatRole = 'user' | 'assistant'

export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  sources?: string[]
}
