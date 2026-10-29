// services/integrations.service.ts
import { http } from './http'
import type {
  CreateIntegrationRequest,
  Integration,
  IntegrationExecution,
  UpdateIntegrationRequest,
} from '@/types'

export const integrationsService = {
  list: () => http.get<Integration[]>('/integrations').then((r) => r.data),

  get: (id: string) =>
    http.get<Integration>(`/integrations/${id}`).then((r) => r.data),

  create: (request: CreateIntegrationRequest) =>
    http.post<Integration>('/integrations', request).then((r) => r.data),

  update: (id: string, request: UpdateIntegrationRequest) =>
    http.put<Integration>(`/integrations/${id}`, request).then((r) => r.data),

  // Toggle de ativação — PATCH dedicado: não reenvia o payload completo
  setActive: (id: string, active: boolean) =>
    http.patch<Integration>(`/integrations/${id}/active`, { active }).then((r) => r.data),

  delete: (id: string) => http.delete<void>(`/integrations/${id}`).then((r) => r.data),

  executions: (id: string) =>
    http.get<IntegrationExecution[]>(`/integrations/${id}/executions`).then((r) => r.data),
}
