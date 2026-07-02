import { useCallback, useEffect, useId, useRef, useState } from 'react'
import {
  Bold,
  Italic,
  Underline,
  List,
  ListOrdered,
  Eraser,
  ChevronDown,
} from 'lucide-react'

/**
 * Cores predefinidas para o botão de cor de texto. Mantemos uma paleta
 * pequena e controlada (em vez de `<input type="color">` puro) para que a
 * seleção seja previsível e compatível com a identidade visual do ERP.
 */
const TEXT_COLORS: ReadonlyArray<{ value: string; label: string }> = [
  { value: '#0f172a', label: 'Preto' }, // slate-900
  { value: '#dc2626', label: 'Vermelho' }, // red-600
  { value: '#2563eb', label: 'Azul' }, // blue-600
  { value: '#16a34a', label: 'Verde' }, // green-600
  { value: '#ca8a04', label: 'Amarelo' }, // yellow-600
  { value: '#ea580c', label: 'Laranja' }, // orange-600
  { value: '#7c3aed', label: 'Roxo' }, // violet-600
  { value: '#64748b', label: 'Cinza' }, // slate-500
]

interface RichTextEditorProps {
  id?: string
  /** Conteúdo HTML controlado. */
  value: string
  onChange: (html: string) => void
  onBlur?: () => void
  placeholder?: string
  /**
   * Limite de caracteres do conteúdo textual (sem tags HTML). Quando o
   * limite é atingido, novos caracteres são bloqueados no `onBeforeInput`.
   */
  maxLength?: number
  className?: string
  /** Quando verdadeiro, desabilita a edição e esconde a toolbar. */
  readOnly?: boolean
}

/**
 * Editor de texto rico leve baseado em `contentEditable`. Não depende de
 * bibliotecas externas. Suporta:
 *
 * - Negrito, itálico, sublinhado
 * - Cor de texto (paleta fixa)
 * - Listas com marcadores e numeradas
 * - Limpar formatação
 *
 * O conteúdo é mantido como string HTML controlada pelo pai via
 * `value`/`onChange`, mesmo formato persistido pelo backend em
 * `quotations.notes` (VARCHAR 2000).
 */
export function RichTextEditor({
  id,
  value,
  onChange,
  onBlur,
  placeholder = 'Digite aqui…',
  maxLength = 2000,
  className = '',
  readOnly = false,
}: RichTextEditorProps) {
  const generatedId = useId()
  const editorId = id ?? generatedId
  const editorRef = useRef<HTMLDivElement>(null)
  const lastValueRef = useRef<string>(value)
  const [colorOpen, setColorOpen] = useState(false)
  const colorPopoverRef = useRef<HTMLDivElement>(null)

  // Sincroniza o DOM com o `value` controlado quando ele muda
  // externamente (ex.: reset do formulário, edição carregada do
  // backend). Usamos um ref para evitar loops com `onInput`.
  useEffect(() => {
    const el = editorRef.current
    if (!el) return
    if (value !== lastValueRef.current) {
      el.innerHTML = value || ''
      lastValueRef.current = value
    }
  }, [value])

  // Fecha o popover de cores ao clicar fora.
  useEffect(() => {
    if (!colorOpen) return
    function handleClickOutside(e: MouseEvent) {
      if (
        colorPopoverRef.current &&
        !colorPopoverRef.current.contains(e.target as Node)
      ) {
        setColorOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [colorOpen])

  const emitChange = useCallback(() => {
    const el = editorRef.current
    if (!el) return
    const html = el.innerHTML
    lastValueRef.current = html
    onChange(html)
  }, [onChange])

  /**
   * Aplica um comando clássico (`document.execCommand`) preservando a
   * seleção atual. Usamos um `mousedown`+`preventDefault` no botão de
   * toolbar para que o editor não perca o foco antes do comando rodar.
   */
  const runCommand = useCallback(
    (command: string, commandArg?: string) => {
      const el = editorRef.current
      if (!el) return
      el.focus()
      // execCommand é considerado deprecated mas continua amplamente
      // suportado e é suficiente para este escopo (sem dependência).
      document.execCommand(command, false, commandArg)
      emitChange()
    },
    [emitChange],
  )

  function handleInput() {
    const el = editorRef.current
    if (!el) return
    if (maxLength > 0) {
      const textLen = el.textContent?.length ?? 0
      if (textLen > maxLength) {
        // Reverte a entrada removendo o último caractere excedente.
        // Simplificação: descartamos a alteração e re-renderizamos a
        // partir do último value conhecido.
        el.innerHTML = lastValueRef.current
        // Reposiciona o cursor no final.
        const range = document.createRange()
        range.selectNodeContents(el)
        range.collapse(false)
        const sel = window.getSelection()
        sel?.removeAllRanges()
        sel?.addRange(range)
        return
      }
    }
    emitChange()
  }

  // Impede que Enter crie <div> no Chromium — usamos <br>.
  function handleKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      document.execCommand('insertLineBreak')
      emitChange()
    }
  }

  function handlePaste(e: React.ClipboardEvent<HTMLDivElement>) {
    // Cola como texto puro para não injetar markup externo.
    e.preventDefault()
    const text = e.clipboardData.getData('text/plain')
    document.execCommand('insertText', false, text)
    emitChange()
  }

  // Mantém o estado da seleção (negrito/itálico/sublinhado ativos) para
  // destacar visualmente os botões correspondentes na toolbar.
  const [, force] = useState(0)
  function refreshActiveStates() {
    force((n) => n + 1)
  }
  function isActive(cmd: 'bold' | 'italic' | 'underline'): boolean {
    try {
      return document.queryCommandState(cmd)
    } catch {
      return false
    }
  }
  const activeColor = (() => {
    try {
      const v = document.queryCommandValue('foreColor')
      // Converte rgb(...) para hex para comparar com nossa paleta.
      if (!v) return null
      const m = v.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/)
      if (!m) return v
      const [, r, g, b] = m
      return (
        '#' +
        [r, g, b]
          .map((n) => Number(n).toString(16).padStart(2, '0'))
          .join('')
      )
    } catch {
      return null
    }
  })()

  const toolbarBtnBase =
    'inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-600 transition-colors ' +
    'hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
  const toolbarBtnActive =
    'bg-slate-200 text-slate-900 dark:bg-slate-700 dark:text-slate-50'

  const isEmpty = !value || value.replace(/<[^>]+>/g, '').trim() === ''

  return (
    <div className={['w-full', className].join(' ')}>
      {/* Toolbar */}
      {!readOnly ? (
        <div
          className={[
            'mb-1.5 flex flex-wrap items-center gap-1 rounded-lg border border-slate-300 bg-white p-1',
            'dark:border-slate-700 dark:bg-slate-900',
          ].join(' ')}
          role="toolbar"
          aria-label="Formatação de texto"
          // Impede roubar o foco do editor antes do execCommand rodar.
          onMouseDown={(e) => e.preventDefault()}
        >
          <button
            type="button"
            title="Negrito (Ctrl+B)"
            aria-label="Negrito"
            aria-pressed={isActive('bold')}
            onClick={() => {
              runCommand('bold')
              refreshActiveStates()
            }}
            className={[toolbarBtnBase, isActive('bold') ? toolbarBtnActive : ''].join(' ')}
          >
            <Bold className="h-4 w-4" />
          </button>
          <button
            type="button"
            title="Itálico (Ctrl+I)"
            aria-label="Itálico"
            aria-pressed={isActive('italic')}
            onClick={() => {
              runCommand('italic')
              refreshActiveStates()
            }}
            className={[toolbarBtnBase, isActive('italic') ? toolbarBtnActive : ''].join(' ')}
          >
            <Italic className="h-4 w-4" />
          </button>
          <button
            type="button"
            title="Sublinhado (Ctrl+U)"
            aria-label="Sublinhado"
            aria-pressed={isActive('underline')}
            onClick={() => {
              runCommand('underline')
              refreshActiveStates()
            }}
            className={[toolbarBtnBase, isActive('underline') ? toolbarBtnActive : ''].join(' ')}
          >
            <Underline className="h-4 w-4" />
          </button>

          <span className="mx-1 h-5 w-px bg-slate-200 dark:bg-slate-700" aria-hidden />

          {/* Cor do texto */}
          <div className="relative" ref={colorPopoverRef}>
            <button
              type="button"
              title="Cor do texto"
              aria-label="Cor do texto"
              aria-expanded={colorOpen}
              onClick={() => setColorOpen((v) => !v)}
              className={[toolbarBtnBase, colorOpen ? toolbarBtnActive : ''].join(' ')}
            >
              <span className="flex items-center gap-0.5">
                <span
                  className="font-bold"
                  style={{ color: activeColor ?? '#0f172a' }}
                  aria-hidden
                >
                  A
                </span>
                <ChevronDown className="h-3 w-3" />
              </span>
            </button>
            {colorOpen ? (
              <div
                className={[
                  'absolute left-0 top-full z-20 mt-1 grid grid-cols-4 gap-1 rounded-lg border border-slate-200 bg-white p-2 shadow-lg',
                  'dark:border-slate-700 dark:bg-slate-900',
                ].join(' ')}
                role="listbox"
                aria-label="Paleta de cores"
              >
                {TEXT_COLORS.map((c) => (
                  <button
                    key={c.value}
                    type="button"
                    role="option"
                    aria-selected={activeColor?.toLowerCase() === c.value.toLowerCase()}
                    title={c.label}
                    onClick={() => {
                      runCommand('foreColor', c.value)
                      setColorOpen(false)
                      refreshActiveStates()
                    }}
                    className={[
                      'inline-flex h-7 w-7 items-center justify-center rounded border border-slate-200 hover:scale-110 transition-transform',
                      'dark:border-slate-700',
                      activeColor?.toLowerCase() === c.value.toLowerCase()
                        ? 'ring-2 ring-focus'
                        : '',
                    ].join(' ')}
                  >
                    <span
                      className="text-base font-bold leading-none"
                      style={{ color: c.value }}
                      aria-hidden
                    >
                      A
                    </span>
                  </button>
                ))}
              </div>
            ) : null}
          </div>

          <span className="mx-1 h-5 w-px bg-slate-200 dark:bg-slate-700" aria-hidden />

          <button
            type="button"
            title="Lista com marcadores"
            aria-label="Lista com marcadores"
            onClick={() => runCommand('insertUnorderedList')}
            className={toolbarBtnBase}
          >
            <List className="h-4 w-4" />
          </button>
          <button
            type="button"
            title="Lista numerada"
            aria-label="Lista numerada"
            onClick={() => runCommand('insertOrderedList')}
            className={toolbarBtnBase}
          >
            <ListOrdered className="h-4 w-4" />
          </button>

          <span className="mx-1 h-5 w-px bg-slate-200 dark:bg-slate-700" aria-hidden />

          <button
            type="button"
            title="Limpar formatação"
            aria-label="Limpar formatação"
            onClick={() => runCommand('removeFormat')}
            className={toolbarBtnBase}
          >
            <Eraser className="h-4 w-4" />
          </button>
        </div>
      ) : null}

      {/* Área editável */}
      <div className="relative">
        <div
          ref={editorRef}
          id={editorId}
          contentEditable={!readOnly}
          suppressContentEditableWarning
          spellCheck
          role="textbox"
          aria-multiline="true"
          aria-readonly={readOnly || undefined}
          aria-label="Observações"
          data-placeholder={placeholder}
          onInput={handleInput}
          onBlur={onBlur}
          onKeyDown={handleKeyDown}
          onPaste={handlePaste}
          onKeyUp={refreshActiveStates}
          onMouseUp={refreshActiveStates}
          className={[
            'min-h-[88px] rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none',
            'dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100',
            'focus:border-focus focus:ring-2 focus:ring-focus/30',
            'transition-[border-color,box-shadow] duration-500 ease-in-out',
            // Estilos para conteúdo gerado pelo editor (mantém aparência
            // consistente em listas e blocos).
            '[&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5',
            '[&_p]:m-0 [&_p+p]:mt-1',
            readOnly ? 'cursor-default' : '',
          ].join(' ')}
        />
        {isEmpty && !readOnly ? (
          <span
            aria-hidden
            className="pointer-events-none absolute left-3 top-2 select-none text-sm text-slate-400 dark:text-slate-500"
          >
            {placeholder}
          </span>
        ) : null}
      </div>
    </div>
  )
}