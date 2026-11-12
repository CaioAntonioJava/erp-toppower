import api from './client'
import type {
  ChangePasswordRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UserResponse,
  UserUpdateRequest,
} from '../types/api'

const BASE = '/api/v1/users'

/**
 * POST /users — cadastra um novo usuário (acesso restrito a ROLE_ADMIN).
 * A role e os módulos são informados no payload.
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

/** GET /users/{id} — busca um usuário por ID (acesso restrito a ROLE_ADMIN). */
export async function getUser(userId: number): Promise<UserResponse> {
  const { data } = await api.get<UserResponse>(`${BASE}/${userId}`)
  return data
}

/** PATCH /users/{id} — atualiza role e/ou módulos de um usuário (ROLE_ADMIN). */
export async function updateUser(
  userId: number,
  payload: UserUpdateRequest,
): Promise<UserResponse> {
  const { data } = await api.patch<UserResponse>(`${BASE}/${userId}`, payload)
  return data
}

/** PATCH /users/{id}/password — troca a senha do próprio usuário. */
export async function changePassword(
  userId: number,
  payload: ChangePasswordRequest,
): Promise<void> {
  await api.patch(`${BASE}/${userId}/password`, payload)
}

/** PATCH /users/{id}/reset-password — admin redefine a senha de qualquer usuário. */
export async function resetUserPassword(
  userId: number,
  payload: ResetPasswordRequest,
): Promise<void> {
  await api.patch(`${BASE}/${userId}/reset-password`, payload)
}

/** DELETE /users/{id} — exclui um usuário e seus vínculos (hard delete). Admin only. */
export async function deleteUser(userId: number): Promise<void> {
  await api.delete(`${BASE}/${userId}`)
}