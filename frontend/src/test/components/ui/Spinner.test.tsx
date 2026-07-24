import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import { Spinner } from '../../../components/ui/Spinner.tsx'

describe('Spinner', () => {
  it('renderiza com aria-label', () => {
    const { getByLabelText } = render(<Spinner />)
    expect(getByLabelText('Carregando')).toBeInTheDocument()
  })

  it('aplica tamanho md por padrão', () => {
    const { getByLabelText } = render(<Spinner />)
    const spinner = getByLabelText('Carregando')
    expect(spinner.getAttribute('class')).toContain('h-6')
    expect(spinner.getAttribute('class')).toContain('w-6')
  })

  it('aplica tamanho sm', () => {
    const { getByLabelText } = render(<Spinner size="sm" />)
    const spinner = getByLabelText('Carregando')
    expect(spinner.getAttribute('class')).toContain('h-4')
  })

  it('aplica tamanho lg', () => {
    const { getByLabelText } = render(<Spinner size="lg" />)
    const spinner = getByLabelText('Carregando')
    expect(spinner.getAttribute('class')).toContain('h-10')
  })
})
