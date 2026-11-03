// services/cardsNotifications.service.ts
import { http } from './http'

export interface NotificationTestResult {
  sent: boolean
  detail: string
  preview: string | null
  cardsFound: number
}

export const cardsNotificationsService = {
  testReminder: () =>
    http.post<NotificationTestResult>('/cards/notifications/test-reminder').then((r) => r.data),

  testDigest: () =>
    http.post<NotificationTestResult>('/cards/notifications/test-digest').then((r) => r.data),
}
