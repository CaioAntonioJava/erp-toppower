import { useCallback, useState } from 'react'

/**
 * Hook que controla a exibição de erros de validação apenas após o usuário
 * ter interagido com o campo (tocado = recebeu foco e perdeu foco) OU após
 * uma tentativa de submit. Isso evita mostrar mensagens de erro em campos
 * que o usuário ainda não visitou.
 *
 * Uso típico:
 *   const { shouldShowError, getBlurHandler, markAllTouched, reset } =
 *     useFieldTouched()
 *
 *   <Input
 *     error={shouldShowError('name', fieldErrors.name)}
 *     onBlur={getBlurHandler('name')}
 *   />
 *
 *   async function handleSubmit(e) {
 *     e.preventDefault()
 *     markAllTouched() // revela erros de todos os campos no submit
 *     if (!validate()) return
 *     ...
 *   }
 */
export function useFieldTouched() {
  const [touched, setTouched] = useState<ReadonlySet<string>>(
    () => new Set<string>(),
  )
  const [submitAttempted, setSubmitAttempted] = useState(false)

  /** Marca um campo como tocado. Idempotente (não causa re-render se já tocado). */
  const markTouched = useCallback((field: string) => {
    setTouched((prev) => {
      if (prev.has(field)) return prev
      const next = new Set(prev)
      next.add(field)
      return next
    })
  }, [])

  /** Handler de onBlur pronto para passar ao Input/Select. */
  const getBlurHandler = useCallback(
    (field: string) => () => markTouched(field),
    [markTouched],
  )

  /**
   * Revela os erros de todos os campos. Deve ser chamado no início do
   * handler de submit para que o usuário veja o que falta preencher.
   */
  const markAllTouched = useCallback(() => {
    setSubmitAttempted(true)
  }, [])

  /** Limpa o estado tocado (ex.: após submit bem-sucedido). */
  const reset = useCallback(() => {
    setTouched(new Set())
    setSubmitAttempted(false)
  }, [])

  /**
   * Devolve o erro apenas se o campo já foi tocado ou se houve tentativa
   * de submit. Caso contrário, devolve `undefined` para não exibir nada.
   */
  const shouldShowError = useCallback(
    <E extends string | null | undefined>(
      field: string,
      error: E,
    ): E | undefined => {
      if (!error) return undefined
      if (submitAttempted || touched.has(field)) return error
      return undefined
    },
    [submitAttempted, touched],
  )

  return {
    /** Set de campos já tocados (útil para debugging ou lógica avançada). */
    touched,
    /** True se o usuário já tentou submeter o formulário. */
    submitAttempted,
    markTouched,
    getBlurHandler,
    markAllTouched,
    reset,
    shouldShowError,
  }
}