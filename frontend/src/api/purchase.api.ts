/**
 * Cliente HTTP do módulo de importação de NF-e.
 *
 * Endpoints sob `/api/v1/purchases/import-xml` (backend: PurchaseImportController).
 */
import api from './client'
import type {
  NfeConfirmRequest,
  NfeConfirmResponse,
  NfePreviewResponse,
} from '../types/purchase'

const BASE = '/api/v1/purchases/import-xml'

/**
 * POST /purchases/import-xml/preview — envia o XML e recebe o preview
 * (fornecedor, produtos, conta a pagar) sem persistir nada.
 */
export async function previewNfeImport(file: File): Promise<NfePreviewResponse> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post<NfePreviewResponse>(`${BASE}/preview`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

/**
 * POST /purchases/import-xml/confirm — confirma a importação enviando
 * o XML em Base64 (retornado no preview). Cria fornecedor, produtos,
 * entrada de estoque e conta a pagar.
 */
export async function confirmNfeImport(
  payload: NfeConfirmRequest,
): Promise<NfeConfirmResponse> {
  const { data } = await api.post<NfeConfirmResponse>(`${BASE}/confirm`, payload)
  return data
}