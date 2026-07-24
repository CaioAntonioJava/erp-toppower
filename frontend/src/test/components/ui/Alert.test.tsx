import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Alert } from '../../../components/ui/Alert.tsx'

describe('Alert', () => {
  it('renderiza o conteúdo', () => {
    render(<Alert>Mensagem de erro</Alert>)
    expect(screen.getByText('Mensagem de erro')).toBeInTheDocument()
  })

  it('usa role alert para variante error', () => {
    render(<Alert variant="error">Erro</Alert>)
    expect(screen.getByRole('alert')).toBeInTheDocument()
  })

  it('usa role status para variante success', () => {
    render(<Alert variant="success">Sucesso</Alert>)
    expect(screen.getByRole('status')).toBeInTheDocument()
  })

  it('usa role status para variante info', () => {
    render(<Alert variant="info">Info</Alert>)
    expect(screen.getByRole('status')).toBeInTheDocument()
  })

  it('aplica classe de cor para error', () => {
    render(<Alert variant="error">Erro</Alert>)
    const alert = screen.getByRole('alert')
    expect(alert.className).toContain('bg-red-50')
  })

  it('aplica classe de cor para success', () => {
    render(<Alert variant="success">Sucesso</Alert>)
    const alert = screen.getByRole('status')
    expect(alert.className).toContain('bg-emerald-50')
  })

  it('aplica classe de cor para info', () => {
    render(<Alert variant="info">Info</Alert>)
    const alert = screen.getByRole('status')
    expect(alert.className).toContain('bg-sky-50')
  })
})
