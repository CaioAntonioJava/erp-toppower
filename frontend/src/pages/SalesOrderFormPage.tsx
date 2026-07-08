import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { SalesOrderForm } from '../components/sales/SalesOrderForm'
import { SalesOrderStatusBadge } from '../components/sales/SalesOrderStatusBadge'
import { StickyFormActions } from '../components/sales/StickyFormActions'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  createSalesOrder,
  getNextSalesOrderNumber,
  getSalesOrder,
  updateSalesOrder,
} from '../api/salesOrder.api'
import type {
  SalesOrderCreateRequest,
  SalesOrderResponse,
  SalesOrderUpdateRequest,
} from '../types/salesOrder'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'
import { useOrganization } from '../context/OrganizationContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um pedido de venda.
 * - /sales-orders/new    → modo create (pode pré-visualizar o próximo número)
 * - /sales-orders/:id    → modo view (carrega GET /sales-orders/{id})
 *
 * <p>Pedidos FINALIZADO ou CANCELADO são somente leitura — o
 * backend rejeita PATCH nesses estados (409).</p>
 */
export function SalesOrderFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'
  // A numeração de pedido é independente por empresa: o próximo número
  // precisa ser re-buscado sempre que a Organization ativa muda (igual ao
  // TechnicalProposalFormPage). Sem isso, ao trocar de empresa no Topbar o
  // número exibido no formulário fica desatualizado.
  const { activeOrganization } = useOrganization()
  const activeOrganizationUuid = activeOrganization?.uuid ?? null

  const [mode, setMode] = useState<Mode>('loading')
  const [salesOrder, setSalesOrder] = useState<SalesOrderResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [nextNumber, setNextNumber] = useState<number | null>(null)

  // Ref do contêiner de ações no cabeçalho. O `StickyFormActions`
  // observa esse elemento: quando ele sai do viewport, o menu sticky
  // aparece; quando volta, some. Mantemos o `null` inicial para que o
  // observer só seja criado quando o elemento estiver montado (após o
  // primeiro render, com `mode !== 'loading'`).
  const actionsRef = useRef<HTMLDivElement>(null)

  // Modo CREATE: pré-visualiza o próximo número para exibir no header.
  // Re-busca sempre que a Organization ativa muda, pois cada empresa tem
  // sua própria sequência (1000, 1001, ... independente por empresa).
  useEffect(() => {
    if (id) return
    if (!activeOrganizationUuid) {
      // Sem Organization ativa: limpa o preview e aguarda seleção no Topbar.
      setMode('create')
      setNextNumber(null)
      return
    }
    let cancelled = false
    setMode('create')
    setNextNumber(null)
    getNextSalesOrderNumber()
      .then((n) => {
        if (cancelled) return
        setNextNumber(n)
      })
      .catch(() => {
        if (cancelled) return
        setNextNumber(null)
      })
    return () => {
      cancelled = true
    }
  }, [id, activeOrganizationUuid])

  // Modo VIEW/EDIT: carrega o pedido pelo ID (imutável entre trocas de org,
  // pois a URL é específica). Em caso de erro, cai em modo create.
  useEffect(() => {
    if (!id) return
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getSalesOrder(id)
      .then((data) => {
        if (cancelled) return
        setSalesOrder(data)
        setMode('view')
      })
      .catch((err) => {
        if (cancelled) return
        setLoadError(toApiError(err).message)
        setMode('create')
      })
    return () => {
      cancelled = true
    }
  }, [id])

  async function handleCreate(payload: SalesOrderCreateRequest) {
    setSaving(true)
    try {
      await createSalesOrder(payload)
      navigate('/sales-orders', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: SalesOrderUpdateRequest) {
    if (!salesOrder) return
    setSaving(true)
    try {
      const updated = await updateSalesOrder(salesOrder.uuid, payload)
      setSalesOrder(updated)
    } finally {
      setSaving(false)
    }
  }

  /** Volta para a lista de pedidos, descartando qualquer edição em andamento. */
  function handleCancel() {
    navigate('/sales-orders')
  }

  // Pedidos em estado terminal não podem ser editados (backend rejeita 409).
  const readOnly =
    mode === 'view' &&
    (salesOrder?.status === 'FINALIZADO' ||
      salesOrder?.status === 'CANCELADO')
  const canEdit = mode === 'view' && !readOnly

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton label="Voltar para a lista" fallback="/sales-orders" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            Pedido de Venda
          </h1>
          {mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para criar o pedido.
            </p>
          ) : mode === 'view' && salesOrder ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span>Emissão: {salesOrder.orderDate}</span>
              <span aria-hidden>•</span>
              <SalesOrderStatusBadge status={salesOrder.status} />
            </div>
          ) : null}
        </div>

        <div ref={actionsRef} className="flex flex-wrap items-center gap-2">
          {readOnly && salesOrder ? (
            <Button
              variant="secondary"
              onClick={() => navigate(`/sales-orders/${salesOrder.uuid}`)}
            >
              Modo somente leitura
            </Button>
          ) : mode !== 'loading' ? (
            <>
              <Button
                type="button"
                variant="secondary"
                onClick={handleCancel}
                size="md"
              >
                <X className="h-4 w-4" />
                Cancelar
              </Button>
              <Button
                type="submit"
                form="sales-order-form"
                isLoading={saving}
                size="md"
                disabled={readOnly}
              >
                <Save className="h-4 w-4" />
                {canEdit ? 'Salvar alterações' : 'Cadastrar pedido'}
              </Button>
            </>
          ) : null}
        </div>
      </div>

      {mode !== 'loading' ? (
        <StickyFormActions
          triggerRef={actionsRef}
          formId="sales-order-form"
          saving={saving}
          readOnly={readOnly}
          canEdit={canEdit}
          onCancel={handleCancel}
          title="PEDIDO DE VENDA"
        />
      ) : null}

      {loadError ? (
        <Alert variant="error">
          {loadError}. <BackButton />
        </Alert>
      ) : null}

      {mode === 'create' && !activeOrganizationUuid ? (
        <Alert variant="info">
          Selecione uma empresa ativa no seletor do topo da tela antes de
          cadastrar um pedido de venda. A numeração e o isolamento dos
          dados dependem da empresa selecionada.
        </Alert>
      ) : null}

      {isAdmin && mode === 'view' && salesOrder ? (
        <RegistrationAuditCard
          createdBy={salesOrder.createdBy}
          createdAt={salesOrder.createdAt}
          updatedBy={salesOrder.updatedBy}
          updatedAt={salesOrder.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="relative">
          {readOnly ? (
            <div className="mb-4">
              <Alert variant="info">
                Pedidos {salesOrder?.status === 'FINALIZADO' ? 'finalizados' : 'cancelados'} não podem ser editados. Use a tela de listagem para visualizar.
              </Alert>
            </div>
          ) : null}
          <fieldset disabled={readOnly} className={readOnly ? 'opacity-70' : ''}>
            {/*
              key = `${activeOrganizationUuid ?? 'no-org'}-${mode}-${id ?? 'new'}`
              força o remount do form sempre que a Organization ativa muda
              (cada empresa tem sua própria sequência de numeração) ou quando
              o modo (create/view/edit) ou o pedido carregado mudam. Sem
              isso, o estado interno do form (incluindo o `number` e o
              `numberDirtyRef`) poderia ficar desatualizado ao trocar de
              empresa pelo dropdown do Topbar.
            */}
            <SalesOrderForm
              key={`${activeOrganizationUuid ?? 'no-org'}-${mode}-${id ?? 'new'}`}
              salesOrder={canEdit ? salesOrder ?? undefined : undefined}
              initialNumber={mode === 'create' ? nextNumber : null}
              onSaveCreate={handleCreate}
              onSaveUpdate={handleUpdate}
            />
          </fieldset>
        </div>
      )}
    </div>
  )
}