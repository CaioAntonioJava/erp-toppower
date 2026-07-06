import api from './client'
import type {
  LoginRequest,
  LoginResponse,
  AuthenticatedUser,
  SwitchTenantRequest,
  TenantSummary,
} from '../types/api'

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

/**
 * GET /auth/tenants?email=... — lista as empresas (tenants) às quais o
 * e-mail informado tem acesso. Endpoint público, usado pela tela de login
 * para popular o dropdown de seleção de empresa antes da autenticação.
 */
export async function listTenantsByEmail(email: string): Promise<TenantSummary[]> {
  const { data } = await api.get<TenantSummary[]>(`${BASE}/tenants`, {
    params: { email },
  })
  return data
}

/**
 * POST /auth/switch-tenant — troca o tenant da sessão corrente,
 * reemitindo o JWT com o novo tenant. Requer token válido.
 */
export async function switchTenant(payload: SwitchTenantRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>(`${BASE}/switch-tenant`, payload)
  return data
}