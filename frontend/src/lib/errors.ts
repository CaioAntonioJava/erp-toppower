import type { ApiError } from '../types/api'

/**
 * Normaliza um erro (tipicamente do axios) na estrutura ApiError do backend.
 *
 * O backend responde com { status, message, timestamp, fieldErrors? }.
 * Esta função extrai essa forma de qualquer erro, caindo em mensagens
 * genéricas para falhas de rede ou formatos inesperados.
 */
export function toApiError(err: unknown): ApiError {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const anyErr = err as any

  const data = anyErr?.response?.data
  if (data && typeof data === 'object' && typeof data.message === 'string') {
    return {
      status: data.status ?? anyErr?.response?.status ?? 0,
      message: data.message,
      timestamp: data.timestamp ?? new Date().toISOString(),
      fieldErrors: data.fieldErrors,
    }
  }

  // Sem resposta do servidor (rede offline, CORS, timeout, etc.)
  if (anyErr?.request) {
    return {
      status: 0,
      message:
        'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.',
      timestamp: new Date().toISOString(),
    }
  }

  return {
    status: 0,
    message: anyErr?.message ?? 'Ocorreu um erro inesperado.',
    timestamp: new Date().toISOString(),
  }
}

/** Mensagem principal de um erro normalizado. */
export function errorMessage(err: unknown): string {
  return toApiError(err).message
}
