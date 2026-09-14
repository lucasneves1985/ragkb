// services/documents.service.ts
import { http } from './http'
import type {
  KnowledgeDocument,
  DocumentActionRequest,
  IngestDocumentRequest,
} from '@/types'

export const documentsService = {
  list: () => http.get<KnowledgeDocument[]>('/documents').then((r) => r.data),

  upload: (data: FormData) =>
    http.post<KnowledgeDocument>('/documents', data, { timeout: 300_000 }).then((r) => r.data),

  ingest: (file: File, request: IngestDocumentRequest) => {
    const formData = new FormData()

    formData.append('file', file)
    formData.append('sector', request.sector)
    formData.append('allowedSectors', JSON.stringify(request.allowedSectors))
    formData.append('allowedRoles', JSON.stringify(request.allowedRoles))

    if (request.supersedesDocumentId) {
      formData.append('supersedesDocumentId', request.supersedesDocumentId)
    }

    return http
      .post<KnowledgeDocument>('/documents', formData, { timeout: 300_000 })
      .then((r) => r.data)
  },

  changeStatus: (id: string, request: DocumentActionRequest) =>
    http.patch<KnowledgeDocument>(`/documents/${id}`, request).then((r) => r.data),
}
