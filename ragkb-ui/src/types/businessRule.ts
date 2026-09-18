// types/businessRule.ts
export type BusinessRuleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type EmbeddingStatus = 'PENDING' | 'DONE' | 'FAILED'

export interface BusinessRuleAttachment {
  id: string
  fileName: string
  contentType: string
  sizeBytes: number
  uploadedBy: string
  uploadedAt: string
}

export interface BusinessRule {
  id: string
  title: string
  description: string
  requester: string | null
  reason: string | null
  status: BusinessRuleStatus
  sectorNames: string[]
  articleIds: string[]
  authorUsername: string
  updatedUsername: string | null
  embeddingStatus: EmbeddingStatus
  createdAt: string
  updatedAt: string
  publishedAt: string | null
}

export interface BusinessRuleActionRequest {
  action: 'PUBLISH' | 'ARCHIVE'
}

export interface CreateBusinessRuleRequest {
  title: string
  description: string
  requester?: string
  reason?: string
  sectorNames: string[]
  articleIds: string[]
}

export interface UpdateBusinessRuleRequest {
  title: string
  description: string
  requester?: string
  reason?: string
  sectorNames: string[]
  articleIds: string[]
}

export interface BusinessRuleSubmitPayload {
  title: string
  description: string
  requester: string
  reason: string
  sectorNames: string[]
  articleIds: string[]
}
