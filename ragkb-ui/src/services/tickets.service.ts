// services/tickets.service.ts
import { http } from './http'
import type { TicketRequest } from '../types'

export const ticketsService = {
  create: (payload: TicketRequest) =>
    http.post('/tickets', payload).then((r) => r.data),
}
