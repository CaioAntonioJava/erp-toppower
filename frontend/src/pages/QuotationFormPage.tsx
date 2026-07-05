import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ClipboardList, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { QuotationForm } from '../components/sales/QuotationForm'
import { QuotationStatusBadge } from '../components/sales/QuotationStatusBadge'
import { StickyFormActions } from '../components/sales/StickyFormActions'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  createQuotation,
  getNextQuotationNumber,
  getQuotation,
  updateQuotation,
} from '../api/quotation.api'
import { createSalesOrderFromQuotation } from '../api/salesOrder.api'
import type {
  QuotationCreateRequest,
  QuotationResponse,
  QuotationUpdateRequest,
} from '../types/quotation'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar uma proposta comercial.
 * - /quotations/new    → modo create (pode pré-visualizar o próximo número)
 * - /quotations/:id    → modo view (carrega GET /quotations/{id})
 */
export function QuotationFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [quotation, setQuotation] = useState<QuotationResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [nextNumber, setNextNumber] = useState<number | null>(null)
  const [confirmConvert, setConfirmConvert] = useState(false)
  const [converting, setConverting] = useState(false)
  const [convertError, setConvertError] = useState<string | null>(null)

  // Ref do contêiner de ações no cabeçalho. O `StickyFormActions`
  // observa esse elemento: quando ele sai do viewport, o menu sticky
  // aparece; quando volta, some. Mantemos o `null` inicial para que o
  // observer só seja criado quando o elemento estiver montado (após o
  // primeiro render, com `mode !== 'loading'`).
  const actionsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!id) {
      setMode('create')
      // Pré-visualiza o próximo número para exibir no header.
      getNextQuotationNumber()
        .then((n) => setNextNumber(n))
        .catch(() => setNextNumber(null))
      return
    }
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getQuotation(id)
      .then((data) => {
        if (cancelled) return
        setQuotation(data)
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

  async function handleCreate(payload: QuotationCreateRequest) {
    setSaving(true)
    try {
      await createQuotation(payload)
      navigate('/quotations', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: QuotationUpdateRequest) {
    if (!quotation) return
    setSaving(true)
    try {
      const updated = await updateQuotation(quotation.uuid, payload)
      setQuotation(updated)
    } finally {
      setSaving(false)
    }
  }

  /** Volta para a lista de propostas, descartando qualquer edição em andamento. */
  function handleCancel() {
    navigate('/quotations')
  }

  /** Converte a proposta ATIVA em pedido de venda e navega para o pedido. */
  async function handleConvert() {
    if (!quotation) return
    setConverting(true)
    setConvertError(null)
    try {
      const order = await createSalesOrderFromQuotation(quotation.uuid)
      navigate(`/sales-orders/${order.uuid}`)
    } catch (err) {
      setConvertError(toApiError(err).message)
    } finally {
      setConverting(false)
    }
  }

  // Propostas CONVERTIDAS não podem ser editadas.
  const readOnly = mode === 'view' && quotation?.status === 'CONVERTIDA'
  const canEdit = mode === 'view' && !readOnly
  // A conversão só faz sentido para propostas ATIVAS em modo de edição.
  const canConvert = mode === 'view' && quotation?.status === 'ATIVA'

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton label="Voltar para a lista" fallback="/quotations" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            Proposta Comercial
          </h1>
          {mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para criar a proposta.
            </p>
          ) : mode === 'view' && quotation ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span>Emissão: {quotation.issueDate}</span>
              <span aria-hidden>•</span>
              <QuotationStatusBadge status={quotation.status} />
            </div>
          ) : null}
        </div>

        <div ref={actionsRef} className="flex flex-wrap items-center gap-2">
          {readOnly && quotation ? (
            <Button
              variant="secondary"
              onClick={() => navigate(`/quotations/${quotation.uuid}`)}
            >
              Modo somente leitura (CONVERTIDA)
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
              {canConvert ? (
                <Button
                  type="button"
                  variant="danger"
                  onClick={() => { setConvertError(null); setConfirmConvert(true) }}
                  size="md"
                >
                  <ClipboardList className="h-4 w-4" />
                  Converter em pedido
                </Button>
              ) : null}
              <Button
                type="submit"
                form="quotation-form"
                isLoading={saving}
                size="md"
                disabled={readOnly}
              >
                <Save className="h-4 w-4" />
                {canEdit ? 'Salvar alterações' : 'Cadastrar proposta'}
              </Button>
            </>
          ) : null}
        </div>
      </div>

      {mode !== 'loading' ? (
        <StickyFormActions
          triggerRef={actionsRef}
          formId="quotation-form"
          saving={saving}
          readOnly={readOnly}
          canEdit={canEdit}
          canConvert={canConvert}
          converting={converting}
          onConvert={() => { setConvertError(null); setConfirmConvert(true) }}
          onCancel={handleCancel}
        />
      ) : null}

      {loadError ? (
        <Alert variant="error">
          {loadError}. <BackButton />
        </Alert>
      ) : null}

      {isAdmin && mode === 'view' && quotation ? (
        <RegistrationAuditCard
          createdBy={quotation.createdBy}
          createdAt={quotation.createdAt}
          updatedBy={quotation.updatedBy}
          updatedAt={quotation.updatedAt}
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
                Propostas convertidas não podem ser editadas. Use a tela de
                listagem para visualizar ou cancelar.
              </Alert>
            </div>
          ) : null}
          <fieldset disabled={readOnly} className={readOnly ? 'opacity-70' : ''}>
            <QuotationForm
              quotation={canEdit ? quotation ?? undefined : undefined}
              initialNumber={mode === 'create' ? nextNumber : null}
              isAdmin={isAdmin}
              onSaveCreate={handleCreate}
              onSaveUpdate={handleUpdate}
            />
          </fieldset>
        </div>
      )}

      {convertError ? (
        <Alert variant="error">{convertError}</Alert>
      ) : null}

      {quotation ? (
        <ConfirmDialog
          open={confirmConvert}
          title="Converter em pedido de venda?"
          description={`A proposta ${quotation.number} será convertida em um novo pedido de venda (status ABERTO). A proposta passará a constar como CONVERTIDA.`}
          confirmText="Converter em pedido"
          confirmVariant="primary"
          isLoading={converting}
          onConfirm={handleConvert}
          onClose={() => {
            if (!converting) setConfirmConvert(false)
          }}
        />
      ) : null}
    </div>
  )
}