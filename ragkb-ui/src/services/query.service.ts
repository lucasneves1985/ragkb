// services/query.service.ts
import { http } from './http'
import type { AskRequest, AnswerResponse } from '../types'

export const queryService = {
  ask: (payload: AskRequest | string, conversationId?: string) => {
    const body: AskRequest =
      typeof payload === 'string' ? { question: payload, conversationId } : payload
    return http.post<AnswerResponse>('/query', body).then((r) => r.data)
  },
}
