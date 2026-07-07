import api from './client'
import type { OrganizationSummary } from '../types/api'

const BASE = '/api/v1/organizations'

/** GET /organizations/mine — lista as Organizations acessíveis ao usuário logado. */
export async function listMine(): Promise<OrganizationSummary[]> {
  const { data } = await api.get<OrganizationSummary[]>(`${BASE}/mine`)
  return data
}