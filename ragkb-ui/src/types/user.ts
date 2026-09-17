// types/user.ts
export interface User {
  id: number
  username: string
  fullName: string
  email: string
  phone: string | null
  roles: string[]
  sectorId: number
  sectorName: string
  enabled: boolean
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
