export type CertificateChannel = 'EMAIL' | 'WHATSAPP'
export interface CertificateMonitor { id:string; url:string; checkTime:string; alertThresholdDays:number; alertWeekdays:string[]; notificationChannel:CertificateChannel; notificationTarget:string; active:boolean; lastCheckedAt:string|null }
export interface CertificateMonitorRequest { url:string; checkTime:string; alertThresholdDays:number; alertWeekdays:string[]; notificationChannel:CertificateChannel; notificationTarget:string; active:boolean }
export interface CertificateCheck { id:string; checkedAt:string; status:'VALID'|'EXPIRING'|'EXPIRED'|'INVALID_CERTIFICATE'|'CONNECTION_ERROR'; daysRemaining:number|null; expiresAt:string|null; checkedUrl:string|null; errorMessage:string|null; alertSent:boolean }
