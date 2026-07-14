import api from './client'
import type {
  OrganizationResponse,
  OrganizationStatus,
  OrganizationSummary,
  OrganizationUpdateRequest,
} from '../types/api'

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

/** GET /organizations (paginado, ADMIN) — lista todas as Organizations com filtros opcionais. */
export async function listAllOrganizations(): Promise<OrganizationSummary[]> {
  // Para a tela administrativa: queremos ver ATIVAS e INATIVAS juntas.
  // O endpoint paginado devolve paged, mas como há tipicamente poucas orgs
  // (2 hoje: Engenharia, Materiais) trazemos página 0 com size grande e
  // retornamos apenas o conteúdo.
  const { data } = await api.get<{ content: OrganizationSummary[] }>(BASE, {
    params: { page: 0, size: 100 },
  })
  return data.content
}

/** GET /organizations/{id} — representação completa (com endereço, telefone, e-mail, logo). */
export async function getOrganization(id: number): Promise<OrganizationResponse> {
  const { data } = await api.get<OrganizationResponse>(`${BASE}/${id}`)
  return data
}

/** PATCH /organizations/{id} — atualização parcial de dados da Organization (admin). */
export async function updateOrganization(
  id: number,
  payload: OrganizationUpdateRequest,
): Promise<OrganizationResponse> {
  const { data } = await api.patch<OrganizationResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /organizations/{id} — inativa a Organization (admin). */
export async function inactivateOrganization(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /organizations/{id}/activate — reativa a Organization (admin). */
export async function activateOrganization(id: number): Promise<OrganizationResponse> {
  const { data } = await api.patch<OrganizationResponse>(`${BASE}/${id}/activate`)
  return data
}

/**
 * POST /organizations/{id}/logo — upload de um arquivo de imagem como
 * logo da Organization. Aceita PNG ou JPEG.
 */
export async function uploadOrganizationLogo(
  id: number,
  file: File,
): Promise<OrganizationResponse> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post<OrganizationResponse>(
    `${BASE}/${id}/logo`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}

/** DELETE /organizations/{id}/logo — remove o logo (arquivo + campo logoUrl). */
export async function deleteOrganizationLogo(
  id: number,
): Promise<OrganizationResponse> {
  const { data } = await api.delete<OrganizationResponse>(`${BASE}/${id}/logo`)
  return data
}

/** Re-exporta tipos auxiliares usados em conjunto com as funções acima. */
export type { OrganizationResponse, OrganizationStatus }