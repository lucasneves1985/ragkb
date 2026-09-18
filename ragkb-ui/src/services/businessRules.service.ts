// services/businessRules.service.ts
import { http } from './http'
import type {
  BusinessRule,
  BusinessRuleAttachment,
  CreateBusinessRuleRequest,
  UpdateBusinessRuleRequest,
} from '@/types'

export const businessRulesService = {
  list: () => http.get<BusinessRule[]>('/business-rules').then((r) => r.data),

  get: (id: string) => http.get<BusinessRule>(`/business-rules/${id}`).then((r) => r.data),

  portal: (q?: string) =>
    http.get<BusinessRule[]>('/business-rules/portal', q ? { params: { q } } : undefined).then((r) => r.data),

  create: (request: CreateBusinessRuleRequest) =>
    http.post<BusinessRule>('/business-rules', request).then((r) => r.data),

  update: (id: string, request: UpdateBusinessRuleRequest) =>
    http.put<BusinessRule>(`/business-rules/${id}`, request).then((r) => r.data),

  publish: (id: string) =>
    http.patch<BusinessRule>(`/business-rules/${id}`, { action: 'PUBLISH' }).then((r) => r.data),

  archive: (id: string) =>
    http.patch<BusinessRule>(`/business-rules/${id}`, { action: 'ARCHIVE' }).then((r) => r.data),

  // ── Anexos ────────────────────────────────────────────────
  listAttachments: (ruleId: string) =>
    http.get<BusinessRuleAttachment[]>(`/business-rules/${ruleId}/attachments`).then((r) => r.data),

  uploadAttachment: (ruleId: string, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http
      .post<BusinessRuleAttachment[]>(`/business-rules/${ruleId}/attachments`, formData)
      .then((r) => r.data)
  },

  deleteAttachment: (ruleId: string, attachmentId: string) =>
    http
      .delete<BusinessRuleAttachment[]>(`/business-rules/${ruleId}/attachments/${attachmentId}`)
      .then((r) => r.data),

  /** Download via blob: o endpoint exige Authorization header — <a href>
   * direto não envia o token. Baixamos autenticado e geramos link local. */
  downloadAttachment: async (ruleId: string, attachment: BusinessRuleAttachment) => {
    const response = await http.get<Blob>(
      `/business-rules/${ruleId}/attachments/${attachment.id}/download`,
      { responseType: 'blob' },
    )
    const url = window.URL.createObjectURL(
      new Blob([response.data], { type: attachment.contentType }),
    )
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  },
}
