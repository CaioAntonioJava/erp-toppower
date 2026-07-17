import { useCallback, useEffect, useState } from 'react'
import { useOrganization } from '../context/OrganizationContext'
import { listBoletos, createBoleto, inactivateBoleto } from '../api/boleto.api'
import { toApiError } from '../lib/errors'
import type { BoletoResponse } from '../types/boleto'
import type { BoletoDue } from '../types/finance'

/**
 * Hook que carrega e gerencia os boletos cadastrados pela usuária,
 * agora conectado ao backend (`/api/v1/boletos`) em substituição ao
 * storage local anterior.
 *
 * Recarrega a lista sempre que a organização ativa muda (o backend já
 * escopa por `organization_id` via header `X-Organization-Id`). O
 * "remove" aqui é o soft delete do backend (inativa o boleto); para
 * manter o dashboard simples, o boleto inativado sai da listagem ativa.
 */

/** Campos do formulário de cadastro de boleto. */
export interface NovoBoletoInput {
  documentNumber: string
  payee: string
  value: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dueDate: string
}

/** Calcula dias até o vencimento a partir de uma data ISO (negativo = vencido). */
function diasAteVencimento(dataVencimento: string): number {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const venc = new Date(dataVencimento)
  venc.setHours(0, 0, 0, 0)
  const msPorDia = 24 * 60 * 60 * 1000
  return Math.round((venc.getTime() - hoje.getTime()) / msPorDia)
}

function derivarStatus(dias: number): BoletoDue['status'] {
  if (dias < 0) return 'ATRASADO'
  return 'ABERTO'
}

/** Converte um BoletoResponse do backend em BoletoDue (com campos derivados). */
function toDue(boleto: BoletoResponse): BoletoDue {
  const dias = diasAteVencimento(boleto.dueDate)
  return {
    id: boleto.id,
    numeroDocumento: boleto.documentNumber,
    pagador: boleto.payee,
    valor: boleto.value,
    dataVencimento: boleto.dueDate,
    diasAteVencimento: dias,
    status: derivarStatus(dias),
  }
}

export function useBoletosStorage() {
  const { activeOrganization } = useOrganization()
  const orgId = activeOrganization?.id
  const [items, setItems] = useState<BoletoDue[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // (Re)carrega do backend sempre que a organização ativa muda.
  const reload = useCallback(async (): Promise<void> => {
    setLoading(true)
    setError(null)
    try {
      // Lista apenas boletos ativos, ordenados por vencimento (default do endpoint).
      const page = await listBoletos({ status: 'ATIVO', size: 100 })
      setItems(page.content.map(toDue))
    } catch (err) {
      setError(toApiError(err).message)
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
    // orgId entra como dependência para recarregar ao trocar de empresa.
  }, [orgId, reload])

  /** Adiciona um boleto cadastrado. Repassa erros do backend ao caller. */
  const add = useCallback(async (input: NovoBoletoInput): Promise<BoletoDue> => {
    const created = await createBoleto({
      documentNumber: input.documentNumber,
      payee: input.payee,
      value: input.value,
      dueDate: input.dueDate,
    })
    const due = toDue(created)
    setItems((prev) => [...prev, due])
    return due
  }, [])

  /** Remove (inativa) um boleto pelo id. Repassa erros do backend ao caller. */
  const remove = useCallback(async (id: number): Promise<void> => {
    await inactivateBoleto(id)
    setItems((prev) => prev.filter((b) => b.id !== id))
  }, [])

  return { items, loading, error, add, remove, reload }
}