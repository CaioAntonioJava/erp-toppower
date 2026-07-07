import api from './client'
import type { OrganizationSummary } from '../types/api'

const BASE = '/api/v1/organizations'

/** GET /organizations/mine — lista as Organizations acessíveis ao usuário logado. */
export async function listMine(): Promise<OrganizationSummary[]> {
  const { data } = await api.get<OrganizationSummary[]>(`${BASE}/mine`)
  return data
}

/** GET /organizations/all — lista todas as Organizations ATIVAS (ADMIN).
 *  Usada no cadastro de usuário para o admin escolher a(s) empresa(s) a vincular. */
export async function listAll(): Promise<OrganizationSummary[]> {
  const { data } = await api.get<OrganizationSummary[]>(`${BASE}/all`)
  return data
}