import api from './client'
import type {
  UserOrganizationAssignRequest,
  UserOrganizationResponse,
} from '../types/api'

const BASE = '/api/v1/user-organizations'

/**
 * POST /api/v1/user-organizations — vincula um usuário a uma Organization.
 * Acesso restrito a ROLE_ADMIN.
 */
export async function assignUserToOrganization(
  payload: UserOrganizationAssignRequest,
): Promise<UserOrganizationResponse> {
  const { data } = await api.post<UserOrganizationResponse>(BASE, payload)
  return data
}

/** GET /api/v1/user-organizations/by-user/{userId} — lista vínculos do usuário. */
export async function listOrganizationsByUser(
  userId: number,
): Promise<UserOrganizationResponse[]> {
  const { data } = await api.get<UserOrganizationResponse[]>(`${BASE}/by-user/${userId}`)
  return data
}

/** DELETE /api/v1/user-organizations/{vinculoId} — remove um vínculo. */
export async function removeUserOrganization(vinculoId: number): Promise<void> {
  await api.delete(`${BASE}/${vinculoId}`)
}