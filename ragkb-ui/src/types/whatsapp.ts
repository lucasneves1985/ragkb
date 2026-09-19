// types/whatsapp.ts
export interface WhatsAppStatus {
  working: boolean
  status: string
}

export interface WhatsAppQr {
  mimetype: string
  data: string
}

export interface WhatsAppTestSendRequest {
  chatId: string
  message: string
}
