// services/reminders.service.ts
import { http } from './http'
import type { Reminder, ReminderCreateRequest } from '@/types'

export const remindersService = {
  list: () => http.get<Reminder[]>('/reminders').then((r) => r.data),

  create: (request: ReminderCreateRequest) =>
    http.post<Reminder>('/reminders', request).then((r) => r.data),

  cancel: (id: string) => http.delete<void>(`/reminders/${id}`).then((r) => r.data),
}
