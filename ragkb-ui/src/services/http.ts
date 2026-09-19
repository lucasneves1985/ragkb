// services/http.ts
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ragkb-token')
  console.log('[axios]', config.method, config.baseURL, config.url, '| token?', !!token)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('ragkb-session')
      localStorage.removeItem('ragkb-token')
      // Preserva a rota atual no redirect — o login devolve para onde
      // o usuário ia (mesmo contrato do guard do router).
      const current = window.location.pathname + window.location.search
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(current)}`
      }
    }
    return Promise.reject(error)
  },
)
