import { http } from './http'
import type { CreateProjectRequest, Project, UpdateProjectRequest, ShareProjectRequest } from '@/types'

export const projectsService = {
  list: () => http.get<Project[]>('/projects').then((r) => r.data),

  get: (id: string) => http.get<Project>(`/projects/${id}`).then((r) => r.data),

  create: (request: CreateProjectRequest) =>
    http.post<Project>('/projects', request).then((r) => r.data),

  update: (id: string, request: UpdateProjectRequest) =>
    http.put<Project>(`/projects/${id}`, request).then((r) => r.data),

  /** 204 — a exclusão cascata para cards, comentários e eventos no backend. */
  delete: (id: string) => http.delete<void>(`/projects/${id}`),

  // ── Compartilhamento (V20) — owner-only no backend ────────
  share: (id: string, request: ShareProjectRequest) =>
    http.post<Project>(`/projects/${id}/shares`, request).then((r) => r.data),

  unshare: (id: string, userId: number) =>
    http.delete<Project>(`/projects/${id}/shares/${userId}`).then((r) => r.data),

  listShares: (id: string) =>
    http.get<string[]>(`/projects/${id}/shares`).then((r) => r.data),
}
