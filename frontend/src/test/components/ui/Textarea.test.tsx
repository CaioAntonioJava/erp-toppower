import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Textarea } from '../../../components/ui/Textarea'

describe('Textarea', () => {
  it('renderiza com label e passa os atributos', () => {
    render(
      <Textarea
        label="Descrição"
        value="cabo 50mm"
        onChange={() => {}}
        aria-label="Descrição do lançamento"
      />,
    )
    expect(screen.getByText('Descrição')).toBeInTheDocument()
    expect(screen.getByLabelText('Descrição do lançamento')).toHaveValue('cabo 50mm')
  })

  it('dispara onChange ao digitar', () => {
    const onChange = vi.fn()
    render(<Textarea value="" onChange={onChange} aria-label="Desc" />)
    fireEvent.change(screen.getByLabelText('Desc'), { target: { value: 'pedágio' } })
    expect(onChange).toHaveBeenCalled()
  })

  it('exibe mensagem de erro e hint mutuamente exclusivos', () => {
    const { rerender } = render(
      <Textarea value="" onChange={() => {}} aria-label="Desc" error="Campo obrigatório" hint="Máx. 500" />,
    )
    expect(screen.getByText('Campo obrigatório')).toBeInTheDocument()
    expect(screen.queryByText('Máx. 500')).not.toBeInTheDocument()

    rerender(<Textarea value="" onChange={() => {}} aria-label="Desc" hint="Máx. 500" />)
    expect(screen.getByText('Máx. 500')).toBeInTheDocument()
  })

  it('renderiza asterisco do required no label', () => {
    render(<Textarea value="" onChange={() => {}} label="Natureza" required aria-label="Nat" />)
    // O label completo contém o asterisco após o texto.
    expect(screen.getByText('Natureza')).toBeInTheDocument()
    const label = screen.getByText('Natureza').closest('label')
    expect(label?.textContent).toContain('*')
  })
})
