export interface Conversation {
  id: string
  title: string
  createdAt: string
  updatedAt?: string
}

export interface ChatMessageItem {
  id: number
  sender: 'USER' | 'ASSISTANT'
  content: string
  status?: string
  sources?: string[]
  ticketSuggestion?: {
    subject: string
    description: string
    requester: string
  }
  createdAt: string
}

export interface ConversationDetail {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: ChatMessageItem[]
}
