// services/conversations.service.ts
import { http } from './http'
import type { Conversation, ConversationDetail } from '../types'

export const conversationsService = {
  list: () => http.get<Conversation[]>('/conversations').then((r) => r.data),
  get: (id: string) => http.get<ConversationDetail>(`/conversations/${id}`).then((r) => r.data),
  remove: (id: string) => http.delete(`/conversations/${id}`),
}
