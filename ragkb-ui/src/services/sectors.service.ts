// services/sectors.service.ts
import { http } from './http'
import type { Sector } from '../types'

export const sectorsService = {
  list: () => http.get<Sector[]>('/sectors').then((r) => r.data),
  create: (name: string) => http.post<Sector>('/sectors', { name }).then((r) => r.data),
  delete: (id: number) => http.delete(`/sectors/${id}`),
}
