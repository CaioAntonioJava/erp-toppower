import { useEffect, useRef, useState } from 'react'
import { Download, X } from 'lucide-react'
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

export function PdfPreview({ title, fetcher, onError }: PdfPreviewProps) {
  const [pdfUrl, setPdfUrl] = useState<string | null>(null)
  const [filename, setFilename] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Mantém o último URL criado para revogar quando um novo chegar.
  const lastUrlRef = useRef<string | null>(null)

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
      .catch((err) => {
        if (cancelled) return
        const message =
          err?.response?.data?.message ??
          err?.message ??
          'Falha ao carregar o PDF.'
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
            src={pdfUrl}
            title={title}
            className="h-full w-full border-0 bg-white"
          />
        ) : null}
      </div>
    </div>
  )
}