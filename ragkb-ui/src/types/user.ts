// types/user.ts
export interface User {
  id: number
  username: string
  fullName: string
  email: string
  avatarUrl: string | null
  phone: string | null
  roles: string[]
  sectorId: number
  sectorName: string
  enabled: boolean
}

/** Entrada leve do diretório (V20) — alimenta o picker de compartilhamento. */
export interface UserDirectoryEntry {
  id: number
  username: string
  fullName: string
}

export interface CreateUserRequest {
  username: string
  password: string
  fullName: string
  email: string
  phone?: string
  roles: string[]
  sectorId: number
}

export interface UpdateUserRequest {
  fullName: string
  email: string
  phone?: string
  roles: string[]
  sectorId: number
  password?: string
  enabled?: boolean
}

export interface UpdateOwnProfileRequest {
  fullName: string
  email: string
}
export interface ChangeOwnPasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}
