// services/users.service.ts
import { http } from './http'
import type { User, CreateUserRequest } from '../types'

export const usersService = {
  list: () => http.get<User[]>('/users').then((r) => r.data),
  create: (
    usernameOrRequest: string | CreateUserRequest,
    password?: string,
    roles?: string[],
    sectorId?: number,
  ) => {
    const payload: CreateUserRequest =
      typeof usernameOrRequest === 'string'
        ? { username: usernameOrRequest, password: password!, roles: roles!, sectorId: sectorId! }
        : usernameOrRequest
    return http.post<User>('/users', payload).then((r) => r.data)
  },
}
