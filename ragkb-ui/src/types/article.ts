// types/article.ts
export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface Article {
  id: string
  title: string
  sector: string
  status: ArticleStatus
  authorUsername: string
  chunkCount: number
  version: number
  createdAt: string
  publishedAt: string | null
  url: string
  allowedSectors: string[]
}

export interface ArticleActionRequest {
  action: 'PUBLISH' | 'ARCHIVE'
}

export interface CreateArticleRequest {
  title: string
  content: string
  sector: string
  allowedSectors: string[]
}

export interface UpdateArticleRequest {
  title: string
  content: string
  allowedSectors: string[]
}

export interface ArticleImageUploadResponse {
  url: string
}

export interface ArticleDetail extends Article {
  content: string
}
