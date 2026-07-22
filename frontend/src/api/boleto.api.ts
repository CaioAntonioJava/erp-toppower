/**
 * Cliente HTTP do módulo de boletos.
 *
 * Endpoints sob `/api/v1/boletos` (backend: BoletoController). Segue a
 * convenção dos demais `*.api.ts`: funções nomeadas, base path por
 * recurso, tipos em `types/boleto.ts`.
 */
import api from './client'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'
import type {
  BoletoAttachmentResponse,
  BoletoCreateRequest,
  BoletoResponse,
  BoletoUpdateRequest,
} from '../types/boleto'

const BASE = '/api/v1/boletos'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /boletos — listagem paginada (status opcional). */
export async function listBoletos(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<BoletoResponse>> {
  const { data } = await api.get<PagedResponse<BoletoResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'dueDate,asc',
      status: params.status,
    },
  })
  return data
}

/**
 * GET /boletos/search — busca flexível.
 * Aceita query e/ou status. Sem nenhum filtro, retorna todos paginados.
 */
export async function searchBoletos(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<BoletoResponse>> {
  const { data } = await api.get<PagedResponse<BoletoResponse>>(
    `${BASE}/search`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'dueDate,asc',
        query: params.query,
        status: params.status,
      },
    },
  )
  return data
}

/** GET /boletos/{id} — detalhe. */
export async function getBoleto(id: number): Promise<BoletoResponse> {
  const { data } = await api.get<BoletoResponse>(`${BASE}/${id}`)
  return data
}

/** POST /boletos — cria um novo boleto. */
export async function createBoleto(
  payload: BoletoCreateRequest,
): Promise<BoletoResponse> {
  const { data } = await api.post<BoletoResponse>(BASE, payload)
  return data
}

/** PATCH /boletos/{id} — atualização parcial. */
export async function updateBoleto(
  id: number,
  payload: BoletoUpdateRequest,
): Promise<BoletoResponse> {
  const { data } = await api.patch<BoletoResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /boletos/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateBoleto(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /boletos/{id}/activate — reativação. */
export async function activateBoleto(id: number): Promise<BoletoResponse> {
  const { data } = await api.patch<BoletoResponse>(`${BASE}/${id}/activate`)
  return data
}

/** POST /boletos/{id}/settle — liquidação (marcar como pago). Aceita comprovante opcional. */
export async function settleBoleto(
  id: number,
  receipt?: File,
): Promise<BoletoResponse> {
  if (receipt) {
    const form = new FormData()
    form.append('receipt', receipt)
    const { data } = await api.post<BoletoResponse>(`${BASE}/${id}/settle`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  }
  const { data } = await api.post<BoletoResponse>(`${BASE}/${id}/settle`)
  return data
}

// =====================================================================
// Anexos de boleto (PDF/imagens) — vários por boleto.
// =====================================================================

/** GET /boletos/{boletoId}/attachments — lista anexos do boleto. */
export async function listBoletoAttachments(
  boletoId: number,
): Promise<BoletoAttachmentResponse[]> {
  const { data } = await api.get<BoletoAttachmentResponse[]>(
    `${BASE}/${boletoId}/attachments`,
  )
  return data
}

/** POST /boletos/{boletoId}/attachments — upload de anexo (multipart). */
export async function uploadBoletoAttachment(
  boletoId: number,
  file: File,
): Promise<BoletoAttachmentResponse> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post<BoletoAttachmentResponse>(
    `${BASE}/${boletoId}/attachments`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}

/** DELETE /boletos/{boletoId}/attachments/{attachmentId} — remove anexo. */
export async function deleteBoletoAttachment(
  boletoId: number,
  attachmentId: number,
): Promise<void> {
  await api.delete(`${BASE}/${boletoId}/attachments/${attachmentId}`)
}

/**
 * Baixa o conteúdo do anexo como Blob (autenticado). Use para exibir
 * inline (preview/impressão) ou forçar download via URL.createObjectURL.
 */
export async function downloadBoletoAttachment(
  boletoId: number,
  attachmentId: number,
  disposition: 'inline' | 'attachment' = 'inline',
): Promise<{ blob: Blob; fileName: string; contentType: string }> {
  const resp = await api.get(`${BASE}/${boletoId}/attachments/${attachmentId}/file`, {
    params: { disposition },
    responseType: 'blob',
  })
  const cdHeader = (resp.headers['content-disposition'] as string | undefined) ?? ''
  const fileNameMatch = /filename="?([^";]+)"?/.exec(cdHeader)
  return {
    blob: resp.data as Blob,
    fileName: fileNameMatch ? fileNameMatch[1] : `boleto-${boletoId}-anexo-${attachmentId}`,
    contentType: (resp.headers['content-type'] as string | undefined) ?? 'application/octet-stream',
  }
}