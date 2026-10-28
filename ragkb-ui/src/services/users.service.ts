// services/users.service.ts
import { http } from './http'
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  UpdateOwnProfileRequest,
  ChangeOwnPasswordRequest,
} from '../types'

export const usersService = {
  me: () => http.get<User>('/users/me').then((r) => r.data),
  updateMe: (payload: UpdateOwnProfileRequest) =>
    http.put<User>('/users/me', payload).then((r) => r.data),
  changeMyPassword: (payload: ChangeOwnPasswordRequest) =>
    http.put<void>('/users/me/password', payload).then((r) => r.data),
  uploadMyAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<User>('/users/me/avatar', formData).then((r) => r.data)
  },
  removeMyAvatar: () => http.delete<User>('/users/me/avatar').then((r) => r.data),
  list: () => http.get<User[]>('/users').then((r) => r.data),
  create: (payload: CreateUserRequest) =>
    http.post<User>('/users', payload).then((r) => r.data),
  update: (id: number, payload: UpdateUserRequest) =>
    http.put<User>(`/users/${id}`, payload).then((r) => r.data),
}

export function resolveAvatarUrl(path: string | null | undefined): string {
  if (!path) return ''
  try {
    const apiBase = new URL(http.defaults.baseURL ?? '/api', window.location.origin)
    return new URL(path, apiBase.origin).toString()
  } catch {
    return path
  }
}
