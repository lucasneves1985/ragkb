// services/cards.service.ts
import { http } from './http'
import type {
  Card,
  CardAttachment,
  CardComment,
  CardDetail,
  CreateCardRequest,
  CreateCommentRequest,
  MoveCardRequest,
  UpdateCardRequest,
  UpdateCommentRequest,
} from '@/types'

export const cardsService = {
  /** Board do projeto. includeClosed controla o toggle "Mostrar encerradas". */
  board: (projectId: string, includeClosed = false) =>
    http
      .get<Card[]>(`/projects/${projectId}/cards`, { params: { includeClosed } })
      .then((r) => r.data),

  detail: (id: string) => http.get<CardDetail>(`/cards/${id}`).then((r) => r.data),

  create: (projectId: string, request: CreateCardRequest) =>
    http.post<Card>(`/projects/${projectId}/cards`, request).then((r) => r.data),

  update: (id: string, request: UpdateCardRequest) =>
    http.put<Card>(`/cards/${id}`, request).then((r) => r.data),

  /** position null = fim da coluna; índice 0-based = posição específica. */
  move: (id: string, request: MoveCardRequest) =>
    http.patch<Card>(`/cards/${id}/status`, request).then((r) => r.data),

  delete: (id: string) => http.delete<void>(`/cards/${id}`),

  addComment: (cardId: string, request: CreateCommentRequest) =>
    http.post<CardComment>(`/cards/${cardId}/comments`, request).then((r) => r.data),

  updateComment: (commentId: string, request: UpdateCommentRequest) =>
    http.put<CardComment>(`/comments/${commentId}`, request).then((r) => r.data),

  deleteComment: (commentId: string) => http.delete<void>(`/comments/${commentId}`),

  uploadAttachment: (cardId: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<CardAttachment[]>(`/cards/${cardId}/attachments`, form).then((r) => r.data)
  },

  deleteAttachment: (cardId: string, attachmentId: string) =>
    http
      .delete<CardAttachment[]>(`/cards/${cardId}/attachments/${attachmentId}`)
      .then((r) => r.data),

  /**
   * Download via blob autenticado — link direto não carrega o Bearer token.
   */
  async downloadAttachment(cardId: string, attachmentId: string, fileName: string): Promise<void> {
    const response = await http.get<Blob>(
      `/cards/${cardId}/attachments/${attachmentId}/download`,
      { responseType: 'blob' },
    )
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = fileName
    anchor.click()
    URL.revokeObjectURL(url)
  },
}
