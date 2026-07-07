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

  const response = anyErr?.response
  const status: number = response?.status ?? 0
  const data = response?.data

  // 1) Resposta JSON com o shape padrão { status, message, timestamp, fieldErrors? }
  if (data && typeof data === 'object' && typeof data.message === 'string') {
    return {
      status: data.status ?? status,
      message: data.message,
      timestamp: data.timestamp ?? new Date().toISOString(),
      fieldErrors: data.fieldErrors,
    }
  }

  // 2) Resposta com corpo textual (string simples, ex.: erro do Tomcat).
  //    Spring às vezes devolve texto puro em 4xx/5xx — ex.: "Forbidden".
  if (typeof data === 'string' && data.trim()) {
    return { status, message: data.trim(), timestamp: new Date().toISOString() }
  }

  // 3) Resposta recebida do servidor, mas sem corpo útil. NÃO confundir
  //    com "sem resposta" — o status já é informação suficiente para o
  //    usuário entender o problema (ex.: 401 = sessão expirou;
  //    403 = sem permissão; 404 = não encontrado; 5xx = erro interno).
  if (response) {
    return {
      status,
      message: defaultMessageForStatus(status),
      timestamp: new Date().toISOString(),
    }
  }

  // 4) Sem resposta do servidor (rede offline, CORS, timeout, backend fora).
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

/**
 * Mensagem padrão por status HTTP, usada quando o backend respondeu
 * com sucesso (request chegou) mas o corpo não tem `message` parseável.
 */
function defaultMessageForStatus(status: number): string {
  if (status === 401) return 'Sessão expirada ou inválida. Faça login novamente.'
  if (status === 403) return 'Acesso negado. Você não tem permissão para esta operação.'
  if (status === 404) return 'Recurso não encontrado.'
  if (status === 409) return 'Conflito: o recurso já existe ou está em estado inválido.'
  if (status === 422) return 'Operação não permitida pelas regras de negócio.'
  if (status >= 500) return 'Erro interno do servidor. Tente novamente em instantes.'
  return `Falha na requisição (HTTP ${status}).`
}

/** Mensagem principal de um erro normalizado. */
export function errorMessage(err: unknown): string {
  return toApiError(err).message
}
