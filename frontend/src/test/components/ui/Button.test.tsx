import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Button } from '../../../components/ui/Button.tsx'

describe('Button', () => {
  it('renderiza o texto', () => {
    render(<Button>Clique aqui</Button>)
    expect(screen.getByText('Clique aqui')).toBeInTheDocument()
  })

  it('renderiza com variante primary por padrão', () => {
    render(<Button>Primary</Button>)
    const btn = screen.getByText('Primary')
    expect(btn.className).toContain('bg-primary')
  })

  it('aplica variante danger', () => {
    render(<Button variant="danger">Danger</Button>)
    const btn = screen.getByText('Danger')
    expect(btn.className).toContain('bg-red-600')
  })

  it('aplica variante ghost', () => {
    render(<Button variant="ghost">Ghost</Button>)
    const btn = screen.getByText('Ghost')
    expect(btn.className).toContain('bg-transparent')
  })

  it('aplica tamanho sm', () => {
    render(<Button size="sm">Small</Button>)
    const btn = screen.getByText('Small')
    expect(btn.className).toContain('h-9')
  })

  it('aplica tamanho lg', () => {
    render(<Button size="lg">Large</Button>)
    const btn = screen.getByText('Large')
    expect(btn.className).toContain('h-12')
  })

  it('aplica fullWidth', () => {
    render(<Button fullWidth>Full</Button>)
    const btn = screen.getByText('Full')
    expect(btn.className).toContain('w-full')
  })

  it('desabilita quando isLoading', () => {
    render(<Button isLoading>Loading</Button>)
    const btn = screen.getByText('Loading')
    expect(btn).toBeDisabled()
  })

  it('desabilita quando disabled', () => {
    render(<Button disabled>Disabled</Button>)
    const btn = screen.getByText('Disabled')
    expect(btn).toBeDisabled()
  })

  it('renderiza como type button por padrão', () => {
    render(<Button>Button</Button>)
    expect(screen.getByText('Button')).toHaveAttribute('type', 'button')
  })

  it('aceita type submit', () => {
    render(<Button type="submit">Submit</Button>)
    expect(screen.getByText('Submit')).toHaveAttribute('type', 'submit')
  })
})
