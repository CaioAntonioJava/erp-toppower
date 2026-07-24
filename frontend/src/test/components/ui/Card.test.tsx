import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import { Card } from '../../../components/ui/Card.tsx'

describe('Card', () => {
  it('renderiza o conteúdo', () => {
    const { getByText } = render(<Card>Conteúdo do card</Card>)
    expect(getByText('Conteúdo do card')).toBeInTheDocument()
  })

  it('aplica padding por padrão', () => {
    const { container } = render(<Card>Com padding</Card>)
    expect(container.firstChild).toHaveClass('p-5')
  })

  it('remove padding quando padded=false', () => {
    const { container } = render(<Card padded={false}>Sem padding</Card>)
    expect(container.firstChild).not.toHaveClass('p-5')
  })
})
