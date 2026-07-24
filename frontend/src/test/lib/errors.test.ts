import { describe, it, expect } from 'vitest'
import { toApiError, errorMessage } from '../../lib/errors'

describe('toApiError', () => {
  it('extrai erro do formato padrão do backend', () => {
    const err = {
      response: {
        status: 422,
        data: {
          status: 422,
          message: 'CNPJ já cadastrado',
          timestamp: '2026-07-24T10:00:00Z',
        },
      },
    }
    const result = toApiError(err)
    expect(result.status).toBe(422)
    expect(result.message).toBe('CNPJ já cadastrado')
    expect(result.timestamp).toBe('2026-07-24T10:00:00Z')
  })

  it('extrai fieldErrors quando presente', () => {
    const err = {
      response: {
        status: 400,
        data: {
          status: 400,
          message: 'Erro de validação',
          timestamp: '2026-07-24T10:00:00Z',
          fieldErrors: { name: 'Nome é obrigatório' },
        },
      },
    }
    const result = toApiError(err)
    expect(result.fieldErrors).toEqual({ name: 'Nome é obrigatório' })
  })

  it('trata resposta com corpo textual', () => {
    const err = {
      response: {
        status: 403,
        data: 'Forbidden',
      },
    }
    const result = toApiError(err)
    expect(result.status).toBe(403)
    expect(result.message).toBe('Forbidden')
  })

  it('usa mensagem padrão para 401 sem corpo', () => {
    const err = { response: { status: 401 } }
    const result = toApiError(err)
    expect(result.message).toContain('Sessão expirada')
  })

  it('usa mensagem padrão para 403 sem corpo', () => {
    const err = { response: { status: 403 } }
    const result = toApiError(err)
    expect(result.message).toContain('Acesso negado')
  })

  it('usa mensagem padrão para 404 sem corpo', () => {
    const err = { response: { status: 404 } }
    const result = toApiError(err)
    expect(result.message).toContain('não encontrado')
  })

  it('usa mensagem padrão para 409 sem corpo', () => {
    const err = { response: { status: 409 } }
    const result = toApiError(err)
    expect(result.message).toContain('Conflito')
  })

  it('usa mensagem padrão para 422 sem corpo', () => {
    const err = { response: { status: 422 } }
    const result = toApiError(err)
    expect(result.message).toContain('regras de negócio')
  })

  it('usa mensagem padrão para 500 sem corpo', () => {
    const err = { response: { status: 500 } }
    const result = toApiError(err)
    expect(result.message).toContain('Erro interno')
  })

  it('trata erro de rede (sem resposta)', () => {
    const err = { request: {} }
    const result = toApiError(err)
    expect(result.status).toBe(0)
    expect(result.message).toContain('conectar ao servidor')
  })

  it('trata erro inesperado', () => {
    const err = new Error('Algo deu errado')
    const result = toApiError(err)
    expect(result.status).toBe(0)
    expect(result.message).toBe('Algo deu errado')
  })

  it('trata erro sem response e sem request', () => {
    const err = { message: 'Erro customizado' }
    const result = toApiError(err)
    expect(result.message).toBe('Erro customizado')
  })
})

describe('errorMessage', () => {
  it('retorna a mensagem do erro normalizado', () => {
    const err = {
      response: {
        status: 400,
        data: { status: 400, message: 'Campo inválido', timestamp: '' },
      },
    }
    expect(errorMessage(err)).toBe('Campo inválido')
  })
})
