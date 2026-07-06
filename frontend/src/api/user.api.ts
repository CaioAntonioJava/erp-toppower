import api from './client'
import type {
  ChangePasswordRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UserResponse,
} from '../types/api'

const BASE = '/api/v1/users'

/**
 * POST /users — cadastra um novo usuário (acesso restrito a ROLE_ADMIN).
 * O backend vincula o usuário ao tenant da sessão do admin e define o papel
 * como ROLE_MANAGER automaticamente.
 */
export async function createUser(payload: RegisterRequest): Promise<UserResponse> {
  const { data } = await api.post<UserResponse>(BASE, payload)
  return data
}

/** GET /users — lista todos os usuários (acesso restrito a ROLE_ADMIN). */
export async function listUsers(): Promise<UserResponse[]> {
  const { data } = await api.get<UserResponse[]>(BASE)
  return data
}

/** PATCH /users/{id}/password — troca a senha do próprio usuário. */
export async function changePassword(
  userId: string,
  payload: ChangePasswordRequest,
): Promise<void> {
  await api.patch(`${BASE}/${userId}/password`, payload)
}

/** PATCH /users/{id}/reset-password — admin redefine a senha de qualquer usuário. */
export async function resetUserPassword(
  userId: string,
  payload: ResetPasswordRequest,
): Promise<void> {
  await api.patch(`${BASE}/${userId}/reset-password`, payload)
}