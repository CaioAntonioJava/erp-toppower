import { useCallback, useEffect, useState } from 'react'
import { useOrganization } from '../context/OrganizationContext'
import { listBoletos, createBoleto, inactivateBoleto, uploadBoletoAttachment, updateBoleto, settleBoleto } from '../api/boleto.api'
import { toApiError } from '../lib/errors'
import type { BoletoResponse, BoletoUpdateRequest } from '../types/boleto'
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
  description: string
  /** Beneficiário do boleto (opcional). */
  payee?: string | null
  value: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). Ignorada quando
   * installmentsCount > 1 (vencimentos derivam de installmentTerms). */
  dueDate: string
  /** ID do fornecedor vinculado. Quando informado, o cadastro do boleto
   * dispara a geração automática de uma conta a pagar no backend. */
  supplierId?: number | null
  /** Anexo opcional (PDF/PNG/JPEG) enviado junto com o cadastro. */
  attachment?: File
  /** Nº de Contrato/Obra vinculado ao boleto (texto livre, opcional). */
  contractWorkNumber?: string | null
  /** Data de cadastro do boleto (ISO yyyy-MM-dd). Default: data atual. */
  registrationDate?: string
  /** Quantidade de parcelas a gerar. Default 1 (boleto avulso). */
  installmentsCount?: number
  /** Prazos das parcelas em dias, separados por barra (ex: "30/60/90"). */
  installmentTerms?: string
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
    descricao: boleto.description,
    pagador: boleto.payee,
    valor: boleto.value,
    dataVencimento: boleto.dueDate,
    diasAteVencimento: dias,
    status: derivarStatus(dias),
    paid: boleto.paid,
    paymentDate: boleto.paymentDate,
    contractWorkNumber: boleto.contractWorkNumber,
    registrationDate: boleto.registrationDate,
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

  /**
   * Adiciona um boleto (ou N boletos quando installmentsCount > 1).
   * Repassa erros do backend ao caller. O backend retorna sempre uma
   * lista (com 1 ou N boletos); todos são inseridos na lista local
   * ordenada por vencimento. Se houver anexo, faz o upload para o
   * primeiro boleto criado (a parcela inicial).
   */
  const add = useCallback(async (input: NovoBoletoInput): Promise<BoletoDue[]> => {
    const created = await createBoleto({
      description: input.description,
      payee: input.payee,
      value: input.value,
      dueDate: input.dueDate,
      supplierId: input.supplierId ?? null,
      contractWorkNumber: input.contractWorkNumber ?? null,
      registrationDate: input.registrationDate,
      installmentsCount: input.installmentsCount,
      installmentTerms: input.installmentTerms,
    })
    const dues = created.map(toDue)
    // Insere mantendo a ordem por vencimento (mais próximo primeiro).
    setItems((prev) =>
      [...prev, ...dues].sort((a, b) => a.dataVencimento.localeCompare(b.dataVencimento)),
    )
    if (input.attachment != null && created.length > 0) {
      // Upload best-effort após o cadastro: o boleto já existe.
      // O anexo é vinculado à primeira parcela criada.
      await uploadBoletoAttachment(created[0].id, input.attachment)
    }
    // Dispara o refresh do dashboard para que os widgets de contas a pagar
    // (e indicadores financeiros) recarreguem e reflitam o novo boleto.
    window.dispatchEvent(new CustomEvent('dashboard:refresh'))
    return dues
  }, [])

  /** Remove (inativa) um boleto pelo id. Repassa erros do backend ao caller. */
  const remove = useCallback(async (id: number): Promise<void> => {
    await inactivateBoleto(id)
    setItems((prev) => prev.filter((b) => b.id !== id))
  }, [])

  /**
   * Atualiza um boleto existente. Chama o PATCH do backend e atualiza
   * o item na lista local com os dados retornados.
   */
  const update = useCallback(async (id: number, input: BoletoUpdateRequest): Promise<BoletoDue> => {
    const updated = await updateBoleto(id, input)
    const due = toDue(updated)
    // Reordena por vencimento, pois a data pode ter sido alterada.
    setItems((prev) => prev.map((b) => (b.id === id ? due : b)).sort((a, b) => a.dataVencimento.localeCompare(b.dataVencimento)))
    return due
  }, [])

  /**
   * Liquida um boleto (marca como pago). Chama o POST /{id}/settle
   * do backend e remove o boleto da lista local — boletos liquidados
   * deixam de aparecer no widget de cadastro e ficam visíveis apenas
   * no relatório (/boletos).
   *
   * Dispara o evento `dashboard:refresh` para que os widgets do
   * dashboard (contas a pagar, indicadores) recarreguem os dados.
   *
   * @param id      ID do boleto
   * @param receipt Comprovante de pagamento opcional (PDF/imagem)
   */
  const settle = useCallback(async (id: number, receipt?: File): Promise<BoletoDue> => {
    const updated = await settleBoleto(id, receipt)
    const due = toDue(updated)
    setItems((prev) => prev.filter((b) => b.id !== id))
    window.dispatchEvent(new CustomEvent('dashboard:refresh'))
    return due
  }, [])

  return { items, loading, error, add, update, settle, remove, reload }
}