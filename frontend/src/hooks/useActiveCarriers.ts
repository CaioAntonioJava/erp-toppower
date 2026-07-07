import { useEffect, useState } from 'react'
import { listCarriers } from '../api/carrier.api'
import type { CarrierResponse } from '../types/carrier'
import type { RegistrationStatus } from '../types/registration'

/** Transportadora atualmente selecionada (modo edição), opcional. */
export interface CurrentCarrier {
  uuid: string
  name?: string | null
}

/**
 * Carrega a lista de transportadoras ativas (status `ATIVO`, size 100) uma
 * vez na montagem do componente, e garante que a transportadora selecionada
 * em modo edição sempre apareça como opção do `<select>`, mesmo que tenha
 * sido inativada após a criação da proposta/pedido.
 *
 * Centraliza a lógica que antes estava duplicada nos três formulários de
 * vendas (Proposta Comercial, Proposta Técnica e Pedido de Vendas).
 */
export function useActiveCarriers(current?: CurrentCarrier | null): {
  carriers: CarrierResponse[]
  carriersLoading: boolean
} {
  const [carriers, setCarriers] = useState<CarrierResponse[]>([])
  const [carriersLoading, setCarriersLoading] = useState(false)

  // Carrega lista de transportadoras ativas uma vez.
  useEffect(() => {
    let cancelled = false
    setCarriersLoading(true)
    listCarriers({ status: 'ATIVO', size: 100, page: 0 })
      .then((p) => {
        if (cancelled) return
        setCarriers(p.content)
      })
      .catch((err) => {
        // Loga para diagnóstico: antes este erro era silenciado, mascarando
        // 403/404 do backend como uma lista vazia no <select> dos formulários.
        console.error('[useActiveCarriers] Falha ao carregar transportadoras:', err)
        if (cancelled) return
        setCarriers([])
      })
      .finally(() => {
        if (!cancelled) setCarriersLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  // Em modo edição, garante que a transportadora atual sempre apareça como
  // opção do <select>, mesmo que tenha sido inativada após a criação do
  // registro. Sem isto, o select cairia no placeholder quando a carrier
  // não retorna no filtro de ATIVO.
  useEffect(() => {
    if (!current?.uuid) return
    setCarriers((prev) => {
      if (prev.some((c) => c.uuid === current.uuid)) return prev
      return [
        ...prev,
        {
          uuid: current.uuid,
          name: current.name ?? '(transportadora removida)',
          status: 'INATIVO' as RegistrationStatus,
          createdAt: '',
          updatedAt: '',
          createdBy: null,
          updatedBy: null,
        },
      ]
    })
  }, [current?.uuid, current?.name])

  return { carriers, carriersLoading }
}