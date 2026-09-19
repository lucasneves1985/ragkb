// types/reminder.ts
export type ReminderStatus = 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED'

export interface Reminder {
  id: string
  conversationId: string | null
  originalRequest: string
  summary: string | null
  remindAt: string
  sentAt: string | null
  status: ReminderStatus
  retryCount: number
  lastError: string | null
  createdAt: string
}

export interface ReminderCreateRequest {
  request: string
  conversationId?: string
}
