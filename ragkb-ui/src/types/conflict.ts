// types/conflict.ts
export type ConflictStatus = 'OPEN' | 'PENDING' | 'DISMISSED' | 'REVIEWED' | 'RESOLVED'

export interface Conflict {
  id: number
  chunkIdA: string
  chunkIdB: string
  documentIdA: string
  documentIdB: string
  score: number
  similarityScore?: number
  snippetA: string
  snippetB: string
  status: ConflictStatus
  note: string
  detectedAt: string
  updatedAt: string
  updatedBy: string
}

export interface ConflictActionRequest {
  action: 'DISMISSED' | 'REVIEWED' | 'RESOLVED'
  note?: string
}
