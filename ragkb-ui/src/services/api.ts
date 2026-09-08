import axios from 'axios'

export type SectorItem = { id: number; name: string }
export type DocumentStatus = 'ACTIVE' | 'ARCHIVED' | 'SUPERSEDED'
export type DocumentItem = {
  id: string
  filename: string
  sector: string
  allowedSectors: string[]
  status: DocumentStatus
  chunkCount: number
  ingestedAt: string
  allowedRoles: string[]
}
export type ConflictItem = {
  id: number
  documentIdA: string
  documentIdB: string
  score: number
  snippetA: string
  snippetB: string
  status: string
  note?: string
}
export type UserItem = {
  id: number
  username: string
  roles: string[]
  sector: string
  enabled: boolean
}
export type QueryResponse =
  | { status: 'KNOWLEDGE'; answer: string; sourceIds: string[]; conversationId: string }
  | {
      status: 'TICKET_SUGGESTED'
      ticketSuggestion: {
        category: string
        subject: string
        description: string
        requester: string
        tags?: string[]
      }
      conversationId: string
    }

export type ConversationSummary = {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export type ChatMessageItem = {
  id: number
  sender: 'USER' | 'ASSISTANT'
  content: string
  status?: string
  sources?: string[]
  ticketSuggestion?: {
    subject: string
    description: string
    requester: string
  }
  createdAt: string
}

export type ConversationDetail = {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: ChatMessageItem[]
}

// Em desenvolvimento, o Vite encaminha /api para localhost:8080 e evita bloqueios de CORS.
// VITE_API_URL permite informar a URL absoluta em ambientes publicados.
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '', timeout: 60_000 })
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('ragkb-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
api.interceptors.response.use(undefined, (error) => {
  if (error.response?.status === 401) {
    sessionStorage.removeItem('ragkb-session')
    sessionStorage.removeItem('ragkb-token')
    if (location.pathname !== '/login') location.assign('/login')
  }
  return Promise.reject(error)
})

export const backend = {
  login: (username: string, password: string) =>
    api.post<{ token: string; expiresIn: number }>('/api/auth/login', { username, password }),
  query: (question: string, conversationId?: string) =>
    api.post<QueryResponse>('/api/query', { question, conversationId }),
  listSectors: () => api.get<SectorItem[]>('/api/sectors'),
  createSector: (name: string) => api.post<SectorItem>('/api/sectors', { name }),
  deleteSector: (id: number) => api.delete(`/api/sectors/${id}`),
  listConversations: () => api.get<ConversationSummary[]>('/api/conversations'),
  getConversation: (id: string) => api.get<ConversationDetail>(`/api/conversations/${id}`),
  createConversation: () => api.post<ConversationDetail>('/api/conversations'),
  deleteConversation: (id: string) => api.delete(`/api/conversations/${id}`),
  listDocuments: () => api.get<DocumentItem[]>('/api/documents'),
  uploadDocument: (data: FormData) =>
    api.post<DocumentItem>('/api/documents', data, { timeout: 300_000 }), // 5 min
  updateDocument: (
    id: string,
    data: { action: 'ARCHIVE' | 'REACTIVATE'; allowedRoles?: string[] },
  ) => api.patch<DocumentItem>(`/api/documents/${id}`, data),
  scanConflicts: () => api.post<ConflictItem[]>('/api/documents/conflicts/scan', {}),
  listConflicts: () => api.get<ConflictItem[]>('/api/documents/conflicts'),
  updateConflict: (id: number, action: 'DISMISSED' | 'REVIEWED' | 'RESOLVED', note?: string) =>
    api.patch<ConflictItem>(`/api/documents/conflicts/${id}`, { action, note }),
  listUsers: () => api.get<UserItem[]>('/api/users'),
  createUser: (username: string, password: string, roles: string[], sector: string) =>
    api.post<UserItem>('/api/users', { username, password, roles, sector }),
  createTicket: (data: { subject: string; description: string; requester: string }) =>
    api.post('/api/tickets', data),
}
