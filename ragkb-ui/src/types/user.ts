// types/user.ts
export interface User {
  id: number
  username: string
  roles: string[]
  sectorId: number | null
  enabled: boolean
}

export interface CreateUserRequest {
  username: string
  password: string
  roles: string[]
  sectorId: number
}
