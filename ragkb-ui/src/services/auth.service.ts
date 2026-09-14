// services/auth.service.ts
import { http } from './http'

export const authService = {
  login: (username: string, password: string) =>
    http
      .post<{ token?: string; accessToken?: string; expiresIn?: number }>('/auth/login', {
        username,
        password,
      })
      .then((r) => r.data),
}
