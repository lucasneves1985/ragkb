// types/integration.ts
export type IntegrationType = 'SCHEDULED' | 'QUERY'
export type IntegrationAuthType = 'NONE' | 'BEARER' | 'BASIC' | 'HEADER_CUSTOM'
export type IntegrationActionType = 'NONE' | 'EMAIL' | 'WHATSAPP'
export type IntegrationExecutionStatus = 'RUNNING' | 'SUCCESS' | 'FAILED'
export type IntegrationHttpMethod = 'GET' | 'POST'


export interface Integration {
  id: string
  name: string
  description: string | null
  url: string
  httpMethod: IntegrationHttpMethod
  authType: IntegrationAuthType
  hasCredentials: boolean
  requestTemplate: string | null
  outputSchema: string | null
  integrationType: IntegrationType
  scheduleCron: string | null
  scheduleTimezone: string | null
  scheduleIntervalSeconds: number | null
  contextDescription: string | null
  paramsDefinition: string | null
  actionType: IntegrationActionType
  actionTarget: string | null
  actionTemplate: string | null
  active: boolean
  createdBy: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface IntegrationExecution {
  id: string
  integrationId: string
  status: IntegrationExecutionStatus
  startedAt: string
  finishedAt: string | null
  attempt: number
  httpStatus: number | null
  responseBody: string | null
  errorMessage: string | null
}



export interface CreateIntegrationRequest {
  name: string
  description?: string
  url: string
  httpMethod: IntegrationHttpMethod
  authType: IntegrationAuthType
  credentials?: string
  requestTemplate?: string
  outputSchema?: string
  integrationType: IntegrationType
  scheduleCron?: string
  scheduleTimezone?: string
  scheduleIntervalSeconds?: number
  contextDescription?: string
  paramsDefinition?: string
  actionType: IntegrationActionType
  actionTarget?: string
  actionTemplate?: string
  active: boolean
}

export interface UpdateIntegrationRequest extends CreateIntegrationRequest {
  /** Blank/null mantém a credencial existente no backend. */
  credentials?: string
}
