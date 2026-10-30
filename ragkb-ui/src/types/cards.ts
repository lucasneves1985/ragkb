// types/cards.ts
export type CardStatus =
  | 'PENDENTE'
  | 'EM_ANDAMENTO'
  | 'AGUARDANDO_FORNECEDOR'
  | 'AGUARDANDO_RETORNO'
  | 'CONCLUIDO'
  | 'CANCELADO'

export const CARD_STATUSES: CardStatus[] = [
  'PENDENTE',
  'EM_ANDAMENTO',
  'AGUARDANDO_FORNECEDOR',
  'AGUARDANDO_RETORNO',
  'CONCLUIDO',
  'CANCELADO',
]

export const CARD_STATUS_LABELS: Record<CardStatus, string> = {
  PENDENTE: 'Pendente',
  EM_ANDAMENTO: 'Em Andamento',
  AGUARDANDO_FORNECEDOR: 'Aguardando Fornecedor',
  AGUARDANDO_RETORNO: 'Aguardando Retorno',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

export const CARD_STATUS_COLORS: Record<CardStatus, string> = {
  PENDENTE: '#909399',
  EM_ANDAMENTO: '#409eff',
  AGUARDANDO_FORNECEDOR: '#e6a23c',
  AGUARDANDO_RETORNO: '#9a6fe0',
  CONCLUIDO: '#67c23a',
  CANCELADO: '#f56c6c',
}

export function isClosedStatus(status: CardStatus): boolean {
  return status === 'CONCLUIDO' || status === 'CANCELADO'
}

export interface Project {
  id: string
  name: string
  description: string | null
  color: string | null
  createdAt: string
  updatedAt: string
}

export interface Card {
  id: string
  projectId: string
  title: string
  description: string | null
  status: CardStatus
  position: number
  closedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CardComment {
  id: string
  content: string
  createdAt: string
}

export interface CardAttachment {
  id: string
  fileName: string
  contentType: string
  sizeBytes: number
  uploadedAt: string
}

export interface CardStatusEvent {
  fromStatus: string | null
  toStatus: string
  changedAt: string
}

export interface CardDetail extends Card {
  comments: CardComment[]
  attachments: CardAttachment[]
  statusHistory: CardStatusEvent[]
}

export interface CreateProjectRequest {
  name: string
  description?: string
  color?: string
}

export interface UpdateProjectRequest {
  name: string
  description?: string
  color?: string
}

export interface CreateCardRequest {
  title: string
  description?: string
  /** Criação estilo Trello direto na coluna; omitido = PENDENTE. */
  status?: CardStatus
}

export interface UpdateCardRequest {
  title: string
  description?: string
}

export interface MoveCardRequest {
  status: CardStatus
  position?: number | null
}

export interface CreateCommentRequest {
  content: string
}

export interface UpdateCommentRequest {
  content: string
}

// ── Assistente de tarefas ─────────────────────────────────

export interface CardAskRequest {
  question: string
  /** Filtro opcional de projeto; omitido busca em todos. */
  projectId?: string
}

export interface CardReference {
  cardId: string
  cardTitle: string
  projectId: string
  projectName: string
  status: CardStatus
}

export interface CardAnswerResponse {
  status: 'ANSWERED' | 'NO_MATCH'
  answer: string
  cards: CardReference[]
}
