// services/whatsapp.service.ts
import { http } from './http'
import type { WhatsAppStatus, WhatsAppTestSendRequest } from '@/types'

export const whatsappService = {
  status: () => http.get<WhatsAppStatus>('/whatsapp/status').then((r) => r.data),

  startSession: () =>
    http.post<{ message: string }>('/whatsapp/session/start').then((r) => r.data),

  testSend: (request: WhatsAppTestSendRequest) =>
    http.post<{ message: string }>('/whatsapp/test-send', request).then((r) => r.data),
}

export const appConfigurationsService = {
  list: () => http.get<Record<string, string>>('/app-configurations').then((r) => r.data),

  set: (key: string, value: string) =>
    http.put<{ message: string }>(`/app-configurations/${key}`, { value }).then((r) => r.data),
}
