// services/conflicts.service.ts
import { http } from './http'
import type { Conflict, ConflictActionRequest } from '../types'

export const conflictsService = {
  list: () => http.get<Conflict[]>('/documents/conflicts').then((r) => r.data),
  scan: () => http.post<Conflict[]>('/documents/conflicts/scan').then((r) => r.data),
  update: (
    id: number,
    actionOrPayload: ConflictActionRequest['action'] | ConflictActionRequest,
    note?: string,
  ) => {
    const payload: ConflictActionRequest =
      typeof actionOrPayload === 'string'
        ? { action: actionOrPayload, note }
        : actionOrPayload
    return http.patch<Conflict>(`/documents/conflicts/${id}`, payload).then((r) => r.data)
  },
}
