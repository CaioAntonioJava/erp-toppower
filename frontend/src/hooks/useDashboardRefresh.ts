import { useCallback, useEffect, useState } from 'react'

/**
 * Hook que escuta o evento `dashboard:refresh` disparado quando um
 * boleto é liquidado (ou qualquer outra ação que deva atualizar os
 * widgets do dashboard).
 *
 * Retorna um contador `refreshKey` que muda a cada evento. Use-o como
 * dependência de `useEffect` nos widgets para reexecutar o fetch.
 *
 * @example
 * const refreshKey = useDashboardRefresh()
 * useEffect(() => { fetchData() }, [refreshKey])
 */
export function useDashboardRefresh(): number {
  const [refreshKey, setRefreshKey] = useState(0)

  const handleRefresh = useCallback(() => {
    setRefreshKey((k) => k + 1)
  }, [])

  useEffect(() => {
    window.addEventListener('dashboard:refresh', handleRefresh)
    return () => window.removeEventListener('dashboard:refresh', handleRefresh)
  }, [handleRefresh])

  return refreshKey
}
