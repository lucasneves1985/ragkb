// services/users.service.ts
import { http } from './http'
import type { User, CreateUserRequest, UpdateUserRequest } from '../types'

export const usersService = {
  list: () => http.get<User[]>('/users').then((r) => r.data),
  create: (payload: CreateUserRequest) =>
    http.post<User>('/users', payload).then((r) => r.data),
  update: (id: number, payload: UpdateUserRequest) =>
    http.put<User>(`/users/${id}`, payload).then((r) => r.data),
}
