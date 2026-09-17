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

export type SourceType = 'DOCUMENT' | 'ARTICLE'

export interface SourceReference {
  type: SourceType
  label: string
  url?: string
}

export interface AnswerResponse {
  status: 'KNOWLEDGE' | 'TICKET_SUGGESTED'
  answer?: string
  sourceIds?: string[]
  sources?: SourceReference[]
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

// Union mantém compatibilidade: histórico persistido carrega strings
// (labels), respostas novas carregam SourceReference
export interface Message {
  id?: string | number
  from?: ChatRole
  role?: ChatRole
  text?: string
  content?: string
  sources?: (string | SourceReference)[]
  ticket?: boolean
  ticketSent?: boolean
  timestamp?: string
}
