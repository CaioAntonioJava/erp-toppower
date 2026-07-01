import api from './client'
import type {
  ChangePasswordRequest,
  RegisterRequest,
  UserResponse,
} from '../types/api'

const BASE = '/api/v1/users'

/**
 * POST /users — endpoint público, usado para o bootstrap de novos usuários.
 * O backend define o papel como ROLE_MANAGER automaticamente.
 */
export async function register(payload: RegisterRequest): Promise<UserResponse> {
  const { data } = await api.post<UserResponse>(BASE, payload)
  return data
}

/** PATCH /users/{id}/password — troca a senha do próprio usuário. */
export async function changePassword(
  userId: string,
  payload: ChangePasswordRequest,
): Promise<void> {
  await api.patch(`${BASE}/${userId}/password`, payload)
}
