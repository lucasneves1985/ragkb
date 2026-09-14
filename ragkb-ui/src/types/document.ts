// types/document.ts
export type DocumentStatus = 'ACTIVE' | 'ARCHIVED' | 'PENDING'

export interface KnowledgeDocument {
  id: string
  filename: string
  sector: string
  allowedSectors: string[]
  status: DocumentStatus
  chunkCount: number
  ingestedAt: string
  allowedRoles: string[]
}

export interface DocumentActionRequest {
  action: 'ARCHIVE' | 'REACTIVATE'
  allowedRoles?: string[]
}

export interface IngestDocumentRequest {
  sector: string
  allowedSectors: string[]
  allowedRoles: string[]
  supersedesDocumentId?: string
}

export interface DocumentUploadPayload {
  file: File
  sector: string
  allowedSectors: string[]
  roles: string[]
  supersedes: string
}
