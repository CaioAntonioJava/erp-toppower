import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Image as ImageIcon,
  Loader2,
  Power,
  Save,
  Trash2,
  Upload,
  X,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { Badge } from '../components/ui/Badge'
import { RichTextEditor } from '../components/ui/RichTextEditor'
import {
  activateOrganization,
  deleteOrganizationLogo,
  getOrganization,
  inactivateOrganization,
  updateOrganization,
  uploadOrganizationLogo,
} from '../api/organization.api'
import type { OrganizationResponse, OrganizationUpdateRequest } from '../types/api'
import { toApiError } from '../lib/errors'
import { BRAZILIAN_STATES } from '../lib/brazilianStates'

const UF_OPTIONS = BRAZILIAN_STATES.map((s) => ({
  value: s.uf,
  label: s.uf,
}))

type Mode = 'loading' | 'create' | 'view'

/** Tipos MIME aceitos pelo backend para o logo. */
const ALLOWED_LOGO_TYPES = ['image/png', 'image/jpeg']
/** Tamanho máximo do logo (deve espelhar `spring.servlet.multipart.max-file-size`). */
const MAX_LOGO_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB

/**
 * Página unificada para visualizar/editar uma Organization (empresa
 * emissora). Acesso restrito a ADMIN.
 *
 * <p>Três modos:</p>
 * <ul>
 *   <li>{@code /organizations/new} → modo create (cadastro).</li>
 *   <li>{@code /organizations/:id} → modo view (carrega {@code GET
 *       /organizations/{id}} e permite edição inline + gestão de logo).</li>
 * </ul>
 *
 * <p>A seção de Logo é a parte mais crítica deste formulário: é ela
 * que garante que o cabeçalho dos PDFs (cotação/proposta/pedido)
 * carregue a marca correta da empresa ativa.</p>
 */
export function OrganizationFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [mode, setMode] = useState<Mode>('loading')
  const [org, setOrg] = useState<OrganizationResponse | null>(null)

  // Estado do formulário (edição)
  const [form, setForm] = useState<OrganizationUpdateRequest>({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saveOk, setSaveOk] = useState(false)

  // Upload do logo
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [uploadingLogo, setUploadingLogo] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)

  // Remover logo
  const [confirmRemoveLogo, setConfirmRemoveLogo] = useState(false)
  const [removingLogo, setRemovingLogo] = useState(false)

  // Inativar / reativar
  const [confirmToggle, setConfirmToggle] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [toggleError, setToggleError] = useState<string | null>(null)

  // Carrega a Organization ao montar (ou entra em modo create)
  useEffect(() => {
    if (!id) {
      // Sem id: a página no momento não suporta create inline porque o
      // backend exige `proposalPrefix` único e CNPJ. Redireciona para
      // a lista com mensagem. (Cadastro via API direta é suficiente
      // por ora; UI de create pode ser adicionada depois.)
      navigate('/organizations', { replace: true })
      return
    }
    let cancelled = false
    setMode('loading')
	    getOrganization(Number(id!))
      .then((data) => {
        if (cancelled) return
        setOrg(data)
        setMode('view')
		        setForm({
		          corporateName: data.corporateName,
		          tradeName: data.tradeName,
		          stateRegistration: data.stateRegistration ?? '',
		          municipalRegistration: data.municipalRegistration ?? '',
		          phone: data.phone ?? '',
		          email: data.email ?? '',
		          zipCode: data.zipCode ?? '',
		          street: data.street ?? '',
		          number: data.number ?? '',
		          district: data.district ?? '',
		          city: data.city ?? '',
		          state: data.state ?? '',
		          complement: data.complement ?? '',
		          proposalPrefix: data.proposalPrefix,
		          contractPrefix: data.contractPrefix,
		          contractDefaultDescription: data.contractDefaultDescription ?? '',
		        })
      })
      .catch((err) => {
        if (!cancelled) setSaveError(toApiError(err).message)
      })
      .finally(() => {
        if (!cancelled) setMode('view')
      })
    return () => { cancelled = true }
  }, [id, navigate])

  // === Handlers de form ===
  function updateField<K extends keyof OrganizationUpdateRequest>(
    key: K,
    value: OrganizationUpdateRequest[K],
  ) {
    setForm((prev: OrganizationUpdateRequest) => ({ ...prev, [key]: value }))
    setSaveOk(false)
  }

  async function handleSave() {
    if (!org) return
    setSaving(true)
    setSaveError(null)
    setSaveOk(false)
    try {
      const updated = await updateOrganization(org.id, form)
      setOrg(updated)
      setSaveOk(true)
    } catch (err) {
      setSaveError(toApiError(err).message)
    } finally {
      setSaving(false)
    }
  }

  // === Handlers de logo ===
  function handlePickFile() {
    fileInputRef.current?.click()
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    // Reseta o input para permitir selecionar o mesmo arquivo novamente
    if (fileInputRef.current) fileInputRef.current.value = ''
    if (!file || !org) return
    setUploadError(null)

    if (!ALLOWED_LOGO_TYPES.includes(file.type)) {
      setUploadError(`Tipo não permitido (${file.type || 'desconhecido'}). Aceitos: PNG ou JPEG.`)
      return
    }
    if (file.size > MAX_LOGO_SIZE_BYTES) {
      setUploadError(`Arquivo muito grande (${(file.size / 1024 / 1024).toFixed(1)} MB). Máximo: 2 MB.`)
      return
    }

    setUploadingLogo(true)
    uploadOrganizationLogo(org.id, file)
      .then((updated) => {
        setOrg(updated)
        setSaveOk(true)
      })
      .catch((err) => {
        setUploadError(toApiError(err).message)
      })
      .finally(() => {
        setUploadingLogo(false)
      })
  }

  async function handleRemoveLogo() {
    if (!org) return
    setRemovingLogo(true)
    setUploadError(null)
    try {
      const updated = await deleteOrganizationLogo(org.id)
      setOrg(updated)
      setConfirmRemoveLogo(false)
    } catch (err) {
      setUploadError(toApiError(err).message)
    } finally {
      setRemovingLogo(false)
    }
  }

  // === Handlers de status ===
  async function handleToggle() {
    if (!org) return
    setToggling(true)
    setToggleError(null)
    try {
      const updated = org.status === 'ATIVO'
	        ? await inactivateOrganization(org.id).then(() => getOrganization(org.id))
	        : await activateOrganization(org.id)
      setOrg(updated)
      setConfirmToggle(false)
    } catch (err) {
      setToggleError(toApiError(err).message)
    } finally {
      setToggling(false)
    }
  }

  if (mode === 'loading') {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!org) {
    return (
      <div className="space-y-4">
        <BackButton />
        <Alert variant="error">{saveError ?? 'Empresa não encontrada.'}</Alert>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            {org.tradeName}
          </h1>
          <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
            <span className="font-mono text-xs">{org.cnpj}</span>
            <span aria-hidden>•</span>
            <Badge tone={org.status === 'ATIVO' ? 'success' : 'neutral'}>
              {org.status === 'ATIVO' ? 'Ativa' : 'Inativa'}
            </Badge>
            <span aria-hidden>•</span>
            <Badge tone="info">Propostas: {org.proposalPrefix}</Badge>
            <Badge tone="info">Contratos: {org.contractPrefix}</Badge>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant={org.status === 'ATIVO' ? 'secondary' : 'primary'}
            onClick={() => {
              setToggleError(null)
              setConfirmToggle(true)
            }}
          >
            <Power className="h-4 w-4" />
            {org.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate('/organizations')}
          >
            <X className="h-4 w-4" />
            Cancelar
          </Button>
          <Button
            type="submit"
            form="org-form"
            isLoading={saving}
          >
            <Save className="h-4 w-4" />
            Salvar alterações
          </Button>
        </div>
      </div>

      {toggleError ? <Alert variant="error">{toggleError}</Alert> : null}

      {/* === Seção: Logo === */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-4 flex items-center gap-2">
          <div className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300">
            <ImageIcon className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-semibold">Logo</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Imagem usada no cabeçalho dos PDFs (cotação, proposta técnica, pedido de venda).
              Formatos: PNG ou JPEG. Máximo 2 MB.
            </p>
          </div>
        </div>

        <div className="flex flex-col items-start gap-4 sm:flex-row sm:items-center">
          {/* Preview do logo atual */}
          <div className="flex h-32 w-48 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-700 dark:bg-slate-800">
            {uploadingLogo ? (
              <Loader2 className="h-6 w-6 animate-spin text-slate-500" />
            ) : org.logoUrl ? (
              <img
                src={org.logoUrl}
                alt={`Logo atual ${org.tradeName}`}
                className="h-full w-full object-contain"
              />
            ) : (
              <div className="flex flex-col items-center gap-1 text-slate-400">
                <ImageIcon className="h-6 w-6" />
                <span className="text-xs">Sem logo</span>
              </div>
            )}
          </div>

          {/* Ações */}
          <div className="flex flex-col gap-2">
            <input
              ref={fileInputRef}
              type="file"
              accept={ALLOWED_LOGO_TYPES.join(',')}
              className="hidden"
              onChange={handleFileChange}
              disabled={uploadingLogo}
            />
            <Button
              type="button"
              onClick={handlePickFile}
              isLoading={uploadingLogo}
            >
              <Upload className="h-4 w-4" />
              {org.logoUrl ? 'Enviar novo logo' : 'Enviar logo'}
            </Button>
            {org.logoUrl ? (
              <Button
                type="button"
                variant="danger"
                onClick={() => {
                  setUploadError(null)
                  setConfirmRemoveLogo(true)
                }}
                disabled={uploadingLogo}
              >
                <Trash2 className="h-4 w-4" />
                Remover logo
              </Button>
            ) : null}
            {org.logoUrl ? (
              <p className="mt-1 max-w-xs break-all text-xs text-slate-500 dark:text-slate-400">
                URL: <code className="rounded bg-slate-100 px-1 py-0.5 dark:bg-slate-800">{org.logoUrl}</code>
              </p>
            ) : (
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Sem logo cadastrado — o template do PDF usa o logo padrão TOP POWER.
              </p>
            )}
          </div>
        </div>

        {uploadError ? <div className="mt-3"><Alert variant="error">{uploadError}</Alert></div> : null}
      </section>

      {/* === Formulário: dados da empresa === */}
      <form
        id="org-form"
        onSubmit={(e) => {
          e.preventDefault()
          handleSave()
        }}
        className="space-y-6"
      >
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-4">
            <h2 className="text-base font-semibold">Dados da empresa</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Razão social, nome fantasia e prefixo das propostas técnicas.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="Razão social"
              value={form.corporateName ?? ''}
              onChange={(e) => updateField('corporateName', e.target.value.toUpperCase())}
              required
              maxLength={200}
            />
            <Input
              label="Nome fantasia"
              value={form.tradeName ?? ''}
              onChange={(e) => updateField('tradeName', e.target.value.toUpperCase())}
              required
              maxLength={200}
            />
            <Input
              label="CNPJ"
              value={org.cnpj}
              readOnly
              hint="CNPJ é imutável após o cadastro (garante unicidade)."
            />
            <Input
              label="Prefixo das propostas técnicas"
              value={form.proposalPrefix ?? ''}
              onChange={(e) =>
                updateField('proposalPrefix', e.target.value.toUpperCase())
              }
              maxLength={10}
              hint={`Ex.: ${form.proposalPrefix ?? 'PT'}-001-${new Date().getFullYear()}. Único no sistema.`}
            />
            <Input
              label="Prefixo dos contratos"
              value={form.contractPrefix ?? ''}
              onChange={(e) =>
                updateField('contractPrefix', e.target.value.toUpperCase())
              }
              maxLength={10}
              hint={`Ex.: ${form.contractPrefix ?? 'CT'}-001-${new Date().getFullYear()}. Único no sistema.`}
            />
            <Input
              label="Inscrição estadual"
              value={form.stateRegistration ?? ''}
              onChange={(e) => updateField('stateRegistration', e.target.value.toUpperCase())}
              maxLength={50}
            />
            <Input
              label="Inscrição municipal"
              value={form.municipalRegistration ?? ''}
              onChange={(e) => updateField('municipalRegistration', e.target.value.toUpperCase())}
              maxLength={50}
            />
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-4">
            <h2 className="text-base font-semibold">Contato</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Telefone e e-mail exibidos no cabeçalho do PDF.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="Telefone"
              value={form.phone ?? ''}
              onChange={(e) => updateField('phone', e.target.value)}
              maxLength={20}
              placeholder="(00) 00000-0000"
            />
            <Input
              label="E-mail"
              type="email"
              value={form.email ?? ''}
              onChange={(e) => updateField('email', e.target.value)}
              maxLength={100}
              placeholder="contato@empresa.com.br"
            />
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-4">
            <h2 className="text-base font-semibold">Endereço</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Endereço exibido no cabeçalho do PDF.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-6">
            <Input
              label="CEP"
              value={form.zipCode ?? ''}
              onChange={(e) => updateField('zipCode', e.target.value)}
              maxLength={9}
              className="sm:col-span-2"
            />
            <Input
              label="Logradouro"
              value={form.street ?? ''}
              onChange={(e) => updateField('street', e.target.value.toUpperCase())}
              maxLength={200}
              className="sm:col-span-4"
            />
            <Input
              label="Número"
              value={form.number ?? ''}
              onChange={(e) => updateField('number', e.target.value)}
              maxLength={20}
              className="sm:col-span-2"
            />
            <Input
              label="Complemento"
              value={form.complement ?? ''}
              onChange={(e) => updateField('complement', e.target.value.toUpperCase())}
              maxLength={100}
              className="sm:col-span-4"
            />
            <Input
              label="Bairro"
              value={form.district ?? ''}
              onChange={(e) => updateField('district', e.target.value.toUpperCase())}
              maxLength={100}
              className="sm:col-span-3"
            />
            <Input
              label="Cidade"
              value={form.city ?? ''}
              onChange={(e) => updateField('city', e.target.value.toUpperCase())}
              maxLength={100}
              className="sm:col-span-2"
            />
            <Select
              label="UF"
              value={form.state ?? ''}
              onChange={(e) => updateField('state', e.target.value)}
              options={[{ value: '', label: 'UF' }, ...UF_OPTIONS]}
              className="sm:col-span-1"
            />
          </div>
        </section>

        {/* Descrição padrão de contratos */}
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-4">
            <h2 className="text-base font-semibold">Descrição padrão de contratos</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Texto HTML que será pré-preenchido automaticamente na descrição
              de novos contratos. O usuário pode editar livremente antes de
              salvar. Use o editor para formatar (negrito, parágrafos, etc.).
            </p>
          </div>
          <RichTextEditor
            value={form.contractDefaultDescription ?? ''}
            onChange={(val) => updateField('contractDefaultDescription', val)}
            maxLength={4000}
            aria-label="Descrição padrão de contratos"
            minHeight={320}
          />
        </section>

        {/* Mensagens de feedback (rodapé do form) */}
        {saveError ? <Alert variant="error">{saveError}</Alert> : null}
        {saveOk && !saveError ? (
          <Alert variant="success">Dados salvos com sucesso.</Alert>
        ) : null}
      </form>

      {/* Modal: remover logo */}
      <ConfirmDialog
        open={confirmRemoveLogo}
        title="Remover logo?"
        description="O arquivo do logo será apagado do disco e o cabeçalho dos PDFs voltará a usar o logo padrão TOP POWER."
        confirmText="Remover"
        confirmVariant="danger"
        isLoading={removingLogo}
        onConfirm={handleRemoveLogo}
        onClose={() => {
          if (!removingLogo) setConfirmRemoveLogo(false)
        }}
      />

      {/* Modal: inativar / reativar */}
      <ConfirmDialog
        open={confirmToggle}
        title={org.status === 'ATIVO' ? 'Inativar empresa?' : 'Reativar empresa?'}
        description={
          org.status === 'ATIVO'
            ? `A empresa "${org.corporateName}" será marcada como inativa. O registro não é apagado e pode ser reativado depois.`
            : `A empresa "${org.corporateName}" voltará a ficar ativa.`
        }
        confirmText={org.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={org.status === 'ATIVO' ? 'danger' : 'primary'}
        isLoading={toggling}
        onConfirm={handleToggle}
        onClose={() => {
          if (!toggling) setConfirmToggle(false)
        }}
      />
    </div>
  )
}