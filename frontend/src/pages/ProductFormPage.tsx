import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Power, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ProductForm } from '../components/client/ProductForm'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  createProduct,
  getProduct,
  inactivateProduct,
  updateProduct,
} from '../api/product.api'
import type {
  ProductCreateRequest,
  ProductResponse,
  ProductUpdateRequest,
} from '../types/product'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um produto.
 * - /products/new        → modo create
 * - /products/:id        → modo view (carrega GET /products/{id})
 */
export function ProductFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmToggle, setConfirmToggle] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [toggleError, setToggleError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      setMode('create')
      return
    }
    let cancelled = false
    setMode('loading')
    setLoadError(null)
	    getProduct(Number(id!))
      .then((data) => {
        if (cancelled) return
        setProduct(data)
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

  async function handleCreate(payload: ProductCreateRequest) {
    setSaving(true)
    try {
      await createProduct(payload)
      // Após salvar, redireciona para a lista (com `replace` para que o
      // botão Voltar do navegador não traga o usuário de volta para o
      // formulário já enviado). O item recém-criado aparecerá na lista
      // após a recarga automática.
      navigate('/products', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: ProductUpdateRequest) {
    if (!product) return
    setSaving(true)
    try {
      const updated = await updateProduct(product.id, payload)
      setProduct(updated)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus() {
    if (!product) return
    setToggling(true)
    setToggleError(null)
    try {
      if (product.status === 'ATIVO') {
        await inactivateProduct(product.id)
	        try {
	          const fresh = await getProduct(product.id)
          setProduct(fresh)
        } catch {
          setProduct({ ...product, status: 'INATIVO' })
        }
      } else {
        // O backend não expõe endpoint dedicado de reativação; usamos
        // o PATCH parcial com `status: 'ATIVO'`, suportado pelo
        // `applyUpdate` (que só escreve campos não-nulos).
        const updated = await updateProduct(product.id, { status: 'ATIVO' })
        setProduct(updated)
      }
    } catch (err) {
      setToggleError(toApiError(err).message)
    } finally {
      setToggling(false)
      setConfirmToggle(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            {mode === 'create' ? 'Novo produto' : product?.name ?? 'Produto'}
          </h1>
          {mode === 'view' && product ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono text-xs">{product.code}</span>
              <span aria-hidden>•</span>
              <RegistrationStatusBadge status={product.status} />
            </div>
          ) : mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para cadastrar um novo produto.
            </p>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {mode === 'view' && product ? (
            <Button
              variant={product.status === 'ATIVO' ? 'secondary' : 'primary'}
              onClick={() => {
                setToggleError(null)
                setConfirmToggle(true)
              }}
            >
              <Power className="h-4 w-4" />
              {product.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
            </Button>
          ) : null}

          {mode !== 'loading' ? (
            <>
              <Button
                type="button"
                variant="secondary"
                onClick={() => navigate('/products')}
                size="md"
              >
                <X className="h-4 w-4" />
                Cancelar
              </Button>
              <Button
                type="submit"
                form="product-form"
                isLoading={saving}
                size="md"
              >
                <Save className="h-4 w-4" />
                {mode === 'view' ? 'Salvar alterações' : 'Cadastrar produto'}
              </Button>
            </>
          ) : null}
        </div>
      </div>

      {loadError ? (
        <Alert variant="error">
          {loadError}.{' '}
          <BackButton />
        </Alert>
      ) : null}

      {isAdmin && mode === 'view' && product ? (
        <RegistrationAuditCard
          createdBy={product.createdBy}
          createdAt={product.createdAt}
          updatedBy={product.updatedBy}
          updatedAt={product.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <ProductForm
          product={mode === 'view' ? product ?? undefined : undefined}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}

      <ConfirmDialog
        open={confirmToggle}
        title={
          product?.status === 'ATIVO' ? 'Inativar produto?' : 'Reativar produto?'
        }
        description={
          product?.status === 'ATIVO'
            ? `O produto "${product?.name}" será marcado como inativo. O registro não é apagado e pode ser reativado depois.`
            : `O produto "${product?.name}" voltará a ficar ativo.`
        }
        confirmText={product?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={product?.status === 'ATIVO' ? 'danger' : 'primary'}
        isLoading={toggling}
        onConfirm={handleToggleStatus}
        onClose={() => {
          if (!toggling) setConfirmToggle(false)
        }}
      />

      {toggleError ? <Alert variant="error">{toggleError}</Alert> : null}
    </div>
  )
}