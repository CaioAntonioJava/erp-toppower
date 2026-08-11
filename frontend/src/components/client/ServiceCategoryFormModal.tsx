import { useEffect, useState, type FormEvent } from 'react'
import { Modal } from '../ui/Modal'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Alert } from '../ui/Alert'
import { createServiceCategory } from '../../api/servicecategory.api'
import type { ServiceCategoryResponse } from '../../types/servicecategory'
import { toApiError } from '../../lib/errors'

interface ServiceCategoryFormModalProps {
  open: boolean
  onClose: () => void
  /** Callback chamado com a categoria recém-criada. Permite ao parent adicioná-la à lista e auto-selecioná-la. */
  onSuccess: (category: ServiceCategoryResponse) => void
}

/**
 * Modal para criação inline de uma categoria de serviço.
 * Usado dentro do ServiceTemplateForm para permitir cadastrar uma nova
 * categoria sem sair do formulário de serviço.
 */
export function ServiceCategoryFormModal({
  open,
  onClose,
  onSuccess,
}: ServiceCategoryFormModalProps) {
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Reseta o estado sempre que o modal é aberto.
  useEffect(() => {
    if (open) {
      setName('')
      setError(null)
      setFieldError(null)
    }
  }, [open])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setFieldError(null)

    if (!name.trim()) {
      setFieldError('Nome é obrigatório.')
      return
    }

    setSubmitting(true)
    try {
      const created = await createServiceCategory({ name: name.trim() })
      onSuccess(created)
      onClose()
    } catch (err) {
      const apiErr = toApiError(err)
      setError(apiErr.message)
      if (apiErr.fieldErrors?.name) {
        setFieldError(apiErr.fieldErrors.name)
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open={open}
      title="Nova categoria de serviço"
      description="Cadastre uma categoria para classificar os serviços do catálogo."
      maxWidth="max-w-md"
      onClose={onClose}
    >
      {error ? <Alert variant="error">{error}</Alert> : null}
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <Input
          label="Nome da categoria"
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={fieldError}
          required
          maxLength={100}
          placeholder="Ex: SPDA"
          hint="O nome será automaticamente convertido para maiúsculas."
          autoFocus
        />
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" isLoading={submitting}>
            Cadastrar categoria
          </Button>
        </div>
      </form>
    </Modal>
  )
}