import api from './client'
import type { LoginRequest, LoginResponse, AuthenticatedUser } from '../types/api'

const BASE = '/api/v1/auth'
const ME_URL = '/api/v1/me'

/** POST /auth/login — autentica e devolve o JWT + dados do usuário. */
export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>(`${BASE}/login`, payload)
  return data
}

/** GET /me — retorna o usuário autenticado a partir do JWT. */
export async function me(): Promise<AuthenticatedUser> {
  const { data } = await api.get<AuthenticatedUser>(ME_URL)
  return data
}
