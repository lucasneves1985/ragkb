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

// União espelha AnswerResponse do backend (frentes 4 e 5):
//   KNOWLEDGE       — resposta via base de conhecimento
//   TICKET_SUGGESTED— conhecimento não localizado OU falha de integração
//   REMINDER_*      — lembrete criado / rejeitado (frente 4)
//   INTEGRATION_RESULT — resultado de integração QUERY (frente 5)
//   PARAM_REQUIRED  — coleta de parâmetro em andamento (fase 2 da frente 5)
export type AnswerStatus =
  | 'KNOWLEDGE'
  | 'TICKET_SUGGESTED'
  | 'REMINDER_CREATED'
  | 'REMINDER_REJECTED'
  | 'INTEGRATION_RESULT'
  | 'PARAM_REQUIRED'

export interface AnswerResponse {
  status: AnswerStatus
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
