/** Helpers para geração e download de CSV no navegador. */

const CSV_NEEDS_QUOTING = /["\n\r,]/

/** Faz o escape de um campo para o formato CSV (RFC 4180). */
function escapeField(value: unknown): string {
  if (value === null || value === undefined) return ''
  const s = String(value)
  if (CSV_NEEDS_QUOTING.test(s)) {
    return `"${s.replace(/"/g, '""')}"`
  }
  return s
}

/** Monta o conteúdo CSV a partir dos cabeçalhos e das linhas. */
export function toCsv(headers: string[], rows: ReadonlyArray<ReadonlyArray<unknown>>): string {
  const headerLine = headers.map(escapeField).join(',')
  const bodyLines = rows.map((row) => row.map(escapeField).join(','))
  // BOM (\ufeff) para que o Excel reconheça UTF-8 corretamente.
  return '\ufeff' + [headerLine, ...bodyLines].join('\n')
}

/** Dispara o download de um CSV no navegador. */
export function downloadCsv(filename: string, csv: string): void {
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/** Gera um nome de arquivo no formato erp-clientes-YYYYMMDD-HHmm.csv. */
export function defaultCsvFilename(prefix: string): string {
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  const stamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(
    now.getDate(),
  )}-${pad(now.getHours())}${pad(now.getMinutes())}`
  return `${prefix}-${stamp}.csv`
}
