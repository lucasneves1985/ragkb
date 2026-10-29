import { http } from './http'
import type { CertificateCheck, CertificateMonitor, CertificateMonitorRequest } from '@/types'
export const certificateMonitorsService = {
  list:()=>http.get<CertificateMonitor[]>('/certificate-monitors').then(r=>r.data),
  create:(payload:CertificateMonitorRequest)=>http.post<CertificateMonitor>('/certificate-monitors',payload).then(r=>r.data),
  update:(id:string,payload:CertificateMonitorRequest)=>http.put<CertificateMonitor>(`/certificate-monitors/${id}`,payload).then(r=>r.data),
  remove:(id:string)=>http.delete(`/certificate-monitors/${id}`),
  history:(id:string)=>http.get<CertificateCheck[]>(`/certificate-monitors/${id}/history`).then(r=>r.data),
  check:(id:string)=>http.post(`/certificate-monitors/${id}/check`),
}
