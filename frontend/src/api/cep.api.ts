import api from './client'
import type { CepResponse } from '../types/cep'

const BASE = '/api/v1/ceps'

/**
 * GET /ceps/{cep} — lookup de endereço por CEP na base local
 * (offline, sem API externa). Requer que a base tenha sido carregada
 * via `POST /api/v1/ceps/import`.
 *
 * @param cep CEP em formato 00000-000 ou 8 dígitos.
 * @throws {ApiError} 404 se o CEP não existir na base local;
 *         400 se o formato for inválido.
 */
export async function getCep(cep: string): Promise<CepResponse> {
  const { data } = await api.get<CepResponse>(`${BASE}/${cep}`)
  return data
}