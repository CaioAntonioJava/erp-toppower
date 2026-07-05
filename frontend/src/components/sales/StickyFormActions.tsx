import { useEffect, useState, type RefObject } from 'react'
import { ClipboardList, Save, X } from 'lucide-react'
import { Button } from '../ui/Button'

interface StickyFormActionsProps {
  /**
   * Ref do contêiner de ações original (cabeçalho da página).
   * Quando esse elemento sai do viewport, o menu sticky aparece.
   */
  triggerRef: RefObject<HTMLElement | null>
  /**
   * `id` do `<form>` que será submetido pelo botão "Salvar".
   * Reaproveita o mesmo submit do cabeçalho — validações e estado
   * continuam centralizados no `QuotationForm`.
   */
  formId: string
  /** Estado de loading do submit. */
  saving: boolean
  /** Proposta em modo somente leitura (ex.: CONVERTIDA). */
  readOnly: boolean
  /** `true` quando estamos editando uma proposta existente. */
  canEdit: boolean
  /** `true` quando a proposta pode ser convertida em pedido (status ATIVA). */
  canConvert?: boolean
  /** Estado de loading da conversão em pedido de venda. */
  converting?: boolean
  /** Handler do botão "Converter em pedido". */
  onConvert?: () => void
  /** Handler do botão "Cancelar". */
  onCancel: () => void
  /** Rótulo curto exibido à esquerda do menu (ex.: "Proposta Comercial"). */
  title?: string
}

/**
 * Menu sticky exibido quando o usuário rola a página além do cabeçalho
 * original. Mostra Salvar/Cancelar para que essas ações fiquem sempre
 * acessíveis em formulários longos (a proposta comercial tem várias
 * seções — cliente, itens, condições, totais — e o submit ficava
 * escondido no final da página).
 *
 * Implementação:
 * - Usa `IntersectionObserver` para detectar quando o cabeçalho sai do
 *   viewport. Quando isso acontece, desliza o menu sticky para dentro;
 *   quando volta, desliza para fora.
 * - `position: fixed; top: 0` para que `-translate-y-full` (que move
 *   100% da altura do próprio menu) o esconda totalmente acima do
 *   viewport — ficar logo abaixo do `Topbar` (`top-16`) não funciona
 *   porque o translate é proporcional à altura do menu, não à do
 *   topbar, e o menu acaba visível por baixo da barra superior.
 *   Quando ativo, o menu cobre o `Topbar` (padrão comum em action
 *   bars de formulários densos).
 * - Quando oculto, fica fora da tela, invisível e removido do tab
 *   order (`-translate-y-full` + `opacity-0` + `invisible` +
 *   `pointer-events-none` + `aria-hidden`) para não interferir em
 *   leitores de tela, foco de teclado nem cliques acidentais.
 */
export function StickyFormActions({
  triggerRef,
  formId,
  saving,
  readOnly,
  canEdit,
  canConvert,
  converting,
  onConvert,
  onCancel,
  title = 'PROPOSTA COMERCIAL',
}: StickyFormActionsProps) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const target = triggerRef.current
    if (!target) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        // Aparece quando o cabeçalho original sai completamente do viewport.
        setVisible(!entry.isIntersecting)
      },
      { threshold: 0 },
    )

    observer.observe(target)
    return () => observer.disconnect()
  }, [triggerRef])

  return (
    <div
      role="region"
      aria-label="Ações do formulário"
      aria-hidden={!visible}
      className={[
        'fixed left-0 right-0 top-0 z-40',
        'transition-all duration-200 ease-out',
        visible
          ? 'translate-y-0 opacity-100 pointer-events-auto'
          : '-translate-y-full opacity-0 pointer-events-none invisible',
      ].join(' ')}
    >
      <div className="border-b border-primary-800 bg-primary-900 shadow-lg shadow-primary/5 backdrop-blur dark:border-slate-700 dark:bg-slate-900/95 dark:shadow-none">
        <div className="mx-auto flex max-w-[1600px] items-center justify-between gap-3 px-4 py-[22px] sm:px-6">
          <span className="truncate text-sm font-semibold uppercase tracking-wide text-white dark:text-slate-200">
            {title}
          </span>
          <div className="flex flex-wrap items-center gap-2">
            {readOnly ? (
              <Button variant="secondary" size="sm" disabled>
                Somente leitura
              </Button>
            ) : (
              <>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={onCancel}
                >
                  <X className="h-4 w-4" />
                  Cancelar
                </Button>
                {canConvert && onConvert ? (
                  <Button
                    type="button"
                    variant="danger"
                    size="sm"
                    isLoading={converting}
                    onClick={onConvert}
                  >
                    <ClipboardList className="h-4 w-4" />
                    Converter em pedido
                  </Button>
                ) : null}
                <Button
                  type="submit"
                  form={formId}
                  size="sm"
                  isLoading={saving}
                  disabled={readOnly}
                >
                  <Save className="h-4 w-4" />
                  {canEdit ? 'Salvar alterações' : 'Cadastrar proposta'}
                </Button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
