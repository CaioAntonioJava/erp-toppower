import api from './client'
import type {
  ProfileCreateRequest,
  ProfileResponse,
  ProfileUpdateRequest,
} from '../types/api'

const BASE = '/api/v1/profiles'

/** GET /profiles/user/{userId} — perfil vinculado a um usuário. */
export async function getProfileByUserId(userId: number): Promise<ProfileResponse> {
  const { data } = await api.get<ProfileResponse>(
    `${BASE}/user/${userId}`,
  )
  return data
}

/** POST /profiles — cria o perfil do usuário autenticado. */
export async function createProfile(
  payload: ProfileCreateRequest,
): Promise<ProfileResponse> {
  const { data } = await api.post<ProfileResponse>(BASE, payload)
  return data
}

/** PATCH /profiles/{id} — atualização parcial do perfil. */
export async function updateProfile(
  id: number,
  payload: ProfileUpdateRequest,
): Promise<ProfileResponse> {
  const { data } = await api.patch<ProfileResponse>(`${BASE}/${id}`, payload)
  return data
}
