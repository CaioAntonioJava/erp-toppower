import { useEffect, useRef, useState } from 'react'
import { Download, Printer, X } from 'lucide-react'
import { Button } from './Button'
import { Spinner } from './Spinner'
import { Alert } from './Alert'

/**
 * Componente compartilhado para preview e download de PDFs gerados
 * pelo backend (cotação, proposta técnica, pedido de venda).
 *
 * <p>Fluxo:</p>
 * <ol>
 *   <li>Recebe um id do documento e um {@link fetcher} que faz o
 *       {@code GET .../pdf} autenticado, retornando
 *       {@code { blob, filename }}.</li>
 *   <li>Cria uma URL de objeto a partir do blob e injeta em um iframe
 *       (preview). Revoga a URL anterior para não vazar memória.</li>
 *   <li>Oferece botão "Baixar PDF" que dispara um download via
 *       {@code <a download>} programático, sem precisar de uma nova
 *       requisição autenticada.</li>
 * </ol>
 *
 * <p>Esse padrão evita colocar o token JWT em uma URL de iframe
 * (riscos de log/history) e mantém o blob apenas em memória do cliente.</p>
 */
interface PdfPreviewProps {
  /** Título exibido acima do iframe (ex.: "Proposta nº 1500"). */
  title: string
  /** Função que baixa o PDF do backend. */
  fetcher: () => Promise<{ blob: Blob; filename: string }>
  /** Callback de erro customizado (opcional). */
  onError?: (message: string) => void
}

/**
 * Lê a mensagem de erro vinda de uma resposta axios com
 * {@code responseType: 'blob'}. Nesse modo, o body de erro (JSON) chega
 * como {@link Blob} em {@code err.response.data}, e o helper precisa
 * decodificá-lo para extrair o campo {@code message} do
 * {@link br.com.toppower.erp_toppower.common.exception.GlobalExceptionHandler.ApiError}.
 *
 * <p>Caminhos cobertos:</p>
 * <ol>
 *   <li>Body já é objeto JSON decodificado (axios padrão);</li>
 *   <li>Body é Blob (axios + responseType:blob);</li>
 *   <li>Body é string (ex.: resposta do Tomcat);</li>
 *   <li>Sem body — usa o status HTTP para mapear uma mensagem padrão.</li>
 * </ol>
 */
async function extractErrorMessage(err: unknown): Promise<string> {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const anyErr = err as any
  const status: number = anyErr?.response?.status ?? 0
  const data = anyErr?.response?.data

  // 1) Blob (caso típico do nosso PDF com responseType: 'blob')
  if (data instanceof Blob) {
    try {
      const text = await data.text()
      if (text) {
        try {
          const parsed = JSON.parse(text)
          if (parsed?.message) return parsed.message
        } catch {
          // Não era JSON — usa o texto cru como fallback
        }
        return text.slice(0, 300)
      }
    } catch {
      // falha ao ler o blob — segue para fallback genérico
    }
  }

  // 2) Objeto JSON já decodificado
  if (data && typeof data === 'object' && typeof data.message === 'string') {
    return data.message
  }

  // 3) String crua
  if (typeof data === 'string' && data.trim()) {
    return data.trim()
  }

  // 4) Fallback por status
  if (status > 0) {
    if (status === 401) return 'Sessão expirada ou inválida. Faça login novamente.'
    if (status === 403) return 'Acesso negado. Você não tem permissão para esta operação.'
    if (status === 404) return 'Recurso não encontrado.'
    if (status === 409) return 'Conflito: o recurso já existe ou está em estado inválido.'
    if (status >= 500) return 'Erro interno do servidor. Tente novamente em instantes.'
    return `Falha na requisição (HTTP ${status}).`
  }

  // 5) Sem resposta do servidor (rede offline, CORS, timeout)
  if (anyErr?.request) {
    return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'
  }

  return anyErr?.message ?? 'Ocorreu um erro inesperado.'
}

export function PdfPreview({ title, fetcher, onError }: PdfPreviewProps) {
  const [pdfUrl, setPdfUrl] = useState<string | null>(null)
  const [filename, setFilename] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Mantém o último URL criado para revogar quando um novo chegar.
  const lastUrlRef = useRef<string | null>(null)

  // Referência ao iframe para acionar a impressão nativa do PDF.
  const iframeRef = useRef<HTMLIFrameElement | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    fetcher()
      .then(({ blob, filename }) => {
        if (cancelled) return
        // Revoga URL anterior antes de criar uma nova.
        if (lastUrlRef.current) {
          URL.revokeObjectURL(lastUrlRef.current)
        }
        const url = URL.createObjectURL(blob)
        lastUrlRef.current = url
        setPdfUrl(url)
        setFilename(filename)
        setLoading(false)
      })
      .catch(async (err) => {
        if (cancelled) return
        // Loga o erro completo no console para facilitar debug futuro.
        // eslint-disable-next-line no-console
        console.error('[PdfPreview] Falha ao carregar PDF:', err)
        const message = await extractErrorMessage(err)
        setError(message)
        onError?.(message)
        setLoading(false)
      })

    return () => {
      cancelled = true
    }
    // fetcher muda a cada render; usamos ref pattern via useRef? Não
    // necessário aqui — a página pai só troca o fetcher quando o id
    // muda (e nesse caso o useEffect é intencionalmente re-executado).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Revoga a URL ao desmontar para liberar memória do browser.
  useEffect(() => {
    return () => {
      if (lastUrlRef.current) {
        URL.revokeObjectURL(lastUrlRef.current)
        lastUrlRef.current = null
      }
    }
  }, [])

  function handleDownload() {
    if (!pdfUrl || !filename) return
    const a = document.createElement('a')
    a.href = pdfUrl
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
  }

  /**
   * Dispara o diálogo de impressão do navegador para o PDF carregado
   * no iframe. Funciona em todos os browsers modernos: o usuário
   * escolhe a impressora destino (ou "Salvar como PDF") e o documento
   * vai direto, sem nova requisição ao backend.
   */
  function handlePrint() {
    if (!pdfUrl) return
    const iframe = iframeRef.current
    // Caminho preferencial: pedir ao iframe para imprimir, preservando
    // margens/orientação do próprio PDF.
    if (iframe && iframe.contentWindow) {
      try {
        iframe.contentWindow.focus()
        iframe.contentWindow.print()
        return
      } catch {
        // Algum browser pode bloquear contentWindow.print(); cai no fallback.
      }
    }
    // Fallback: abre o blob em uma nova aba e dispara a impressão lá.
    const win = window.open(pdfUrl, '_blank', 'noopener,noreferrer')
    if (win) {
      // Dá tempo do PDF carregar antes de abrir o diálogo.
      win.addEventListener('load', () => {
        try {
          win.focus()
          win.print()
        } catch {
          // Sem permissão — o usuário pode usar Ctrl+P manualmente.
        }
      })
    }
  }

  return (
    <div className="flex h-screen flex-col">
      {/* Barra de ações — não interfere com o iframe. */}
      <div className="flex flex-shrink-0 items-center justify-between border-b border-slate-200 bg-white px-6 py-3 shadow-sm">
        <div className="min-w-0">
          <h1 className="truncate text-base font-semibold text-slate-900">
            {title}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleDownload}
            disabled={!pdfUrl || loading}
          >
            <Download className="h-4 w-4" />
            Baixar PDF
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={handlePrint}
            disabled={!pdfUrl || loading}
            title="Imprimir proposta"
          >
            <Printer className="h-4 w-4" />
            Imprimir Proposta
          </Button>
          <Button variant="ghost" size="sm" onClick={() => window.close()}>
            <X className="h-4 w-4" />
            Fechar
          </Button>
        </div>
      </div>

      {/* Área de conteúdo: spinner, alerta ou iframe. */}
      <div className="flex-1 overflow-hidden bg-slate-100">
        {loading ? (
          <div className="flex h-full items-center justify-center">
            <Spinner size="lg" />
          </div>
        ) : error ? (
          <div className="m-6">
            <Alert variant="error">{error}</Alert>
          </div>
        ) : pdfUrl ? (
          <iframe
            ref={iframeRef}
            src={pdfUrl}
            title={title}
            className="h-full w-full border-0 bg-white"
          />
        ) : null}
      </div>
    </div>
  )
}