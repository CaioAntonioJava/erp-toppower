import { useCallback, useEffect, useId, useRef, useState } from 'react'
import {
  Bold,
  Italic,
  Underline,
  List,
  ListOrdered,
  Eraser,
  ChevronDown,
  Type,
  TextSelect,
} from 'lucide-react'

/**
 * Cores predefinidas para o botão de cor de texto. Mantemos uma paleta
 * pequena e controlada (em vez de `<input type="color">` puro) para que a
 * seleção seja previsível e compatível com a identidade visual do ERP.
 */
const TEXT_COLORS: ReadonlyArray<{ value: string; label: string }> = [
  { value: '#ffffff', label: 'Branco' },
  { value: '#dc2626', label: 'Vermelho' }, // red-600
  { value: '#ca8a04', label: 'Amarelo' }, // yellow-600
]

/**
 * Fontes disponíveis para o seletor de fonte. A primeira entrada é a
 * fonte padrão (sem style aplicado).
 */
const FONT_FAMILIES: ReadonlyArray<{ value: string; label: string }> = [
  { value: '', label: 'Padrão' },
  { value: 'Arial, sans-serif', label: 'Arial' },
  { value: '"Times New Roman", serif', label: 'Times New Roman' },
  { value: 'Calibri, sans-serif', label: 'Calibri' },
  { value: 'Tahoma, sans-serif', label: 'Tahoma' },
  { value: 'Verdana, sans-serif', label: 'Verdana' },
  { value: '"Courier New", monospace', label: 'Courier New' },
]

/**
 * Tamanhos de fonte disponíveis para o seletor de tamanho. O valor vazio
 * representa o tamanho padrão (sem style aplicado).
 */
const FONT_SIZES: ReadonlyArray<{ value: string; label: string }> = [
  { value: '', label: 'Padrão' },
  { value: '8pt', label: '8' },
  { value: '9pt', label: '9' },
  { value: '10pt', label: '10' },
  { value: '11pt', label: '11' },
  { value: '12pt', label: '12' },
  { value: '14pt', label: '14' },
  { value: '16pt', label: '16' },
  { value: '18pt', label: '18' },
  { value: '20pt', label: '20' },
  { value: '22pt', label: '22' },
  { value: '24pt', label: '24' },
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
  /** Altura mínima da área editável em pixels. Padrão: 120. */
  minHeight?: number
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
  minHeight = 120,
}: RichTextEditorProps) {
  const generatedId = useId()
  const editorId = id ?? generatedId
  const editorRef = useRef<HTMLDivElement>(null)
  const lastValueRef = useRef<string>('')
  const [colorOpen, setColorOpen] = useState(false)
  const colorPopoverRef = useRef<HTMLDivElement>(null)
  const [fontFamilyOpen, setFontFamilyOpen] = useState(false)
  const fontFamilyPopoverRef = useRef<HTMLDivElement>(null)
  const [fontSizeOpen, setFontSizeOpen] = useState(false)
  const fontSizePopoverRef = useRef<HTMLDivElement>(null)

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

  // Fecha o popover de fonte ao clicar fora.
  useEffect(() => {
    if (!fontFamilyOpen) return
    function handleClickOutside(e: MouseEvent) {
      if (
        fontFamilyPopoverRef.current &&
        !fontFamilyPopoverRef.current.contains(e.target as Node)
      ) {
        setFontFamilyOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [fontFamilyOpen])

  // Fecha o popover de tamanho ao clicar fora.
  useEffect(() => {
    if (!fontSizeOpen) return
    function handleClickOutside(e: MouseEvent) {
      if (
        fontSizePopoverRef.current &&
        !fontSizePopoverRef.current.contains(e.target as Node)
      ) {
        setFontSizeOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [fontSizeOpen])

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

  /**
   * Aplica um style CSS (fontFamily ou fontSize) à seleção atual,
   * envolvendo o conteúdo selecionado em um <span> com o style
   * desejado. Usamos esta abordagem em vez de `execCommand('fontSize')`
   * porque este só aceita valores 1-7 (HTML), não pt/px.
   */
  const wrapWithStyle = useCallback(
    (property: 'fontFamily' | 'fontSize', value: string) => {
      const el = editorRef.current
      if (!el) return
      el.focus()
      const sel = window.getSelection()
      if (!sel || sel.isCollapsed || !sel.rangeCount) return
      const range = sel.getRangeAt(0)
      const span = document.createElement('span')
      if (property === 'fontFamily') span.style.fontFamily = value
      else span.style.fontSize = value
      try {
        const fragment = range.extractContents()
        span.appendChild(fragment)
        range.insertNode(span)
        // Restaura a seleção para o novo span.
        sel.removeAllRanges()
        const newRange = document.createRange()
        newRange.selectNodeContents(span)
        sel.addRange(newRange)
      } catch {
        // Fallback silencioso para seleções inválidas.
      }
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
    const activeFontFamily = (() => {
      try {
        return document.queryCommandValue('fontName') || ''
      } catch {
        return ''
      }
    })()
    const activeFontSize = (() => {
      try {
        // Tenta obter o tamanho do elemento pai da seleção.
        const sel = window.getSelection()
        if (!sel || !sel.rangeCount) return ''
        const node = sel.getRangeAt(0).startContainer
        const el = node.nodeType === Node.TEXT_NODE ? node.parentElement : node as Element
        if (el instanceof HTMLElement) {
          const size = el.style.fontSize
          if (size) return size
          // Se não tem style direto, sobe na árvore.
          let parent = el.parentElement
          while (parent) {
            const s = parent.style.fontSize
            if (s) return s
            parent = parent.parentElement
          }
        }
        return ''
      } catch {
        return ''
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
                  className="text-sm font-bold leading-none"
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
	                  'absolute left-0 top-full z-20 mt-1 flex gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-lg',
	                  'dark:border-slate-700 dark:bg-slate-900',
	                ].join(' ')}
	                role="listbox"
	                aria-label="Paleta de cores"
	              >
	                {TEXT_COLORS.map((c) => {
	                  const isSelected = activeColor?.toLowerCase() === c.value.toLowerCase()
	                  return (
	                    <button
	                      key={c.value}
	                      type="button"
	                      role="option"
	                      aria-selected={isSelected}
	                      title={c.label}
	                      onClick={() => {
	                        runCommand('foreColor', c.value)
	                        setColorOpen(false)
	                        refreshActiveStates()
	                      }}
	                      className={[
	                        'inline-flex h-8 w-8 items-center justify-center rounded-md transition-all',
	                        'hover:bg-slate-100 dark:hover:bg-slate-800',
	                        isSelected
	                          ? 'bg-slate-200 dark:bg-slate-700 ring-2 ring-focus'
	                          : '',
	                      ].join(' ')}
	                    >
	                      <span
	                        className="text-lg font-bold leading-none"
	                        style={{ color: c.value }}
	                        aria-hidden
	                      >
	                        A
	                      </span>
	                    </button>
	                  )
	                })}
	              </div>
            ) : null}
          </div>

          <span className="mx-1 h-5 w-px bg-slate-200 dark:bg-slate-700" aria-hidden />

          {/* Fonte */}
          <div className="relative" ref={fontFamilyPopoverRef}>
            <button
              type="button"
              title="Fonte"
              aria-label="Fonte"
              aria-expanded={fontFamilyOpen}
              onClick={() => setFontFamilyOpen((v) => !v)}
              className={[toolbarBtnBase, fontFamilyOpen ? toolbarBtnActive : ''].join(' ')}
            >
              <span className="flex items-center gap-0.5">
                <Type className="h-4 w-4" />
                <ChevronDown className="h-3 w-3" />
              </span>
            </button>
            {fontFamilyOpen ? (
              <div
                className={[
                  'absolute left-0 top-full z-20 mt-1 min-w-[180px] rounded-lg border border-slate-200 bg-white p-2 shadow-lg',
                  'dark:border-slate-700 dark:bg-slate-900',
                ].join(' ')}
                role="listbox"
                aria-label="Selecionar fonte"
              >
                {FONT_FAMILIES.map((f) => {
                  const isSelected = activeFontFamily === f.value
                  return (
                    <button
                      key={f.value}
                      type="button"
                      role="option"
                      aria-selected={isSelected}
                      title={f.label}
                      onClick={() => {
                        if (f.value) {
                          wrapWithStyle('fontFamily', f.value)
                        } else {
                          // "Padrão" — remove o style fontFamily do elemento pai.
                          const el = editorRef.current
                          if (el) {
                            el.focus()
                            const sel = window.getSelection()
                            if (sel && !sel.isCollapsed && sel.rangeCount) {
                              const range = sel.getRangeAt(0)
                              const parent = range.startContainer.parentElement
                              if (parent) {
                                parent.style.fontFamily = ''
                                if (!parent.getAttribute('style')) {
                                  parent.removeAttribute('style')
                                }
                              }
                            }
                            emitChange()
                          }
                        }
                        setFontFamilyOpen(false)
                        refreshActiveStates()
                      }}
                      className={[
                        'flex w-full items-center rounded-md px-3 py-1.5 text-left text-sm transition-colors',
                        'hover:bg-slate-100 dark:hover:bg-slate-800',
                        isSelected ? 'bg-slate-200 font-medium dark:bg-slate-700' : '',
                      ].join(' ')}
                      style={f.value ? { fontFamily: f.value } : undefined}
                    >
                      {f.label}
                    </button>
                  )
                })}
              </div>
            ) : null}
          </div>

          {/* Tamanho da fonte */}
          <div className="relative" ref={fontSizePopoverRef}>
            <button
              type="button"
              title="Tamanho da fonte"
              aria-label="Tamanho da fonte"
              aria-expanded={fontSizeOpen}
              onClick={() => setFontSizeOpen((v) => !v)}
              className={[toolbarBtnBase, fontSizeOpen ? toolbarBtnActive : ''].join(' ')}
            >
              <span className="flex items-center gap-0.5">
                <TextSelect className="h-4 w-4" />
                <ChevronDown className="h-3 w-3" />
              </span>
            </button>
            {fontSizeOpen ? (
              <div
                className={[
                  'absolute left-0 top-full z-20 mt-1 min-w-[120px] rounded-lg border border-slate-200 bg-white p-2 shadow-lg',
                  'dark:border-slate-700 dark:bg-slate-900',
                ].join(' ')}
                role="listbox"
                aria-label="Selecionar tamanho da fonte"
              >
                {FONT_SIZES.map((s) => {
                  const isSelected = activeFontSize === s.value
                  return (
                    <button
                      key={s.value}
                      type="button"
                      role="option"
                      aria-selected={isSelected}
                      title={`${s.label} pt`}
                      onClick={() => {
                        if (s.value) {
                          wrapWithStyle('fontSize', s.value)
                        } else {
                          // "Padrão" — remove o style fontSize.
                          const el = editorRef.current
                          if (el) {
                            el.focus()
                            const sel = window.getSelection()
                            if (sel && !sel.isCollapsed && sel.rangeCount) {
                              const range = sel.getRangeAt(0)
                              const parent = range.startContainer.parentElement
                              if (parent) {
                                parent.style.fontSize = ''
                                if (!parent.getAttribute('style')) {
                                  parent.removeAttribute('style')
                                }
                              }
                            }
                            emitChange()
                          }
                        }
                        setFontSizeOpen(false)
                        refreshActiveStates()
                      }}
                      className={[
                        'flex w-full items-center rounded-md px-3 py-1.5 text-left text-sm transition-colors',
                        'hover:bg-slate-100 dark:hover:bg-slate-800',
                        isSelected ? 'bg-slate-200 font-medium dark:bg-slate-700' : '',
                      ].join(' ')}
                    >
                      <span style={s.value ? { fontSize: s.value } : undefined}>
                        {s.label}
                      </span>
                    </button>
                  )
                })}
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
          style={{ minHeight: `${minHeight}px` }}
          className={[
            'rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none',
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