import api from './client'
import type { TenantResponse, TenantSummary } from '../types/api'

const BASE = '/api/v1/tenants'

/**
 * Converte um TenantResponse (DTO completo do backend) em TenantSummary,
 * aplicando a mesma regra de displayName usada no backend TenantMapper:
 * tradeName se preenchido, senão legalName.
 */
function toSummary(t: TenantResponse): TenantSummary {
  const displayName =
    t.tradeName && t.tradeName.trim() !== '' ? t.tradeName : t.legalName
  return {
    uuid: t.uuid,
    displayName,
    code: t.code,
    cnpj: t.cnpj,
  }
}

/**
 * GET /tenants — lista todas as empresas (tenants). Acesso restrito a
 * ROLE_ADMIN. Usado pelo formulário de cadastro de usuário para popular
 * a seleção de empresas às quais o novo usuário terá acesso.
 */
export async function listTenants(): Promise<TenantSummary[]> {
  const { data } = await api.get<TenantResponse[]>(BASE)
  return data.map(toSummary)
}