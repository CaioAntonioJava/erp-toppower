import axios from 'axios'

/**
 * Instância central do axios.
 *
 * - baseURL: usa VITE_API_URL em produção; em dev omitimos para que as
 *   chamadas /api passem pelo proxy do Vite (sem CORS).
 * - Os interceptors injetam o JWT e tratam o 401 (token inválido/expirado).
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '',
  headers: { 'Content-Type': 'application/json' },
})

/** Chave usada para persistir o token no localStorage. */
export const TOKEN_KEY = 'erp_toppower_token'

/** Chave usada para persistir a Organization ativa no localStorage. */
export const ORGANIZATION_KEY = 'erp_toppower_org'

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // Injeta a Organization ativa em toda requisição autenticada.
  // O backend valida acesso; endpoints de gestão (login, /me, /organizations)
  // toleram o header ausente.
  const orgId = localStorage.getItem(ORGANIZATION_KEY)
  if (orgId) {
    config.headers['X-Organization-Id'] = orgId
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401: limpa a sessão e redireciona para o login.
    // Evita loop quando a própria chamada já é do fluxo de login.
    const status = error?.response?.status
    const url: string = error?.config?.url ?? ''
    if (status === 401 && !url.includes('/auth/login')) {
      localStorage.removeItem(TOKEN_KEY)
      // Redirecionamento direto no window para desacoplar do roteador.
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

export default api
