import { http } from './http'
import type { CreateProjectRequest, Project, UpdateProjectRequest } from '@/types'

export const projectsService = {
  list: () => http.get<Project[]>('/projects').then((r) => r.data),

  get: (id: string) => http.get<Project>(`/projects/${id}`).then((r) => r.data),

  create: (request: CreateProjectRequest) =>
    http.post<Project>('/projects', request).then((r) => r.data),

  update: (id: string, request: UpdateProjectRequest) =>
    http.put<Project>(`/projects/${id}`, request).then((r) => r.data),

  /** 204 — a exclusão cascata para cards, comentários e eventos no backend. */
  delete: (id: string) => http.delete<void>(`/projects/${id}`),
}
