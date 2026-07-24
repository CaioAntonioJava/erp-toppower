import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Badge } from '../../../components/ui/Badge.tsx'

describe('Badge', () => {
  it('renderiza o texto', () => {
    render(<Badge>ATIVO</Badge>)
    expect(screen.getByText('ATIVO')).toBeInTheDocument()
  })

  it('aplica tom neutral por padrão', () => {
    render(<Badge>Neutral</Badge>)
    const badge = screen.getByText('Neutral')
    expect(badge.className).toContain('bg-slate-100')
  })

  it('aplica tom success', () => {
    render(<Badge tone="success">Success</Badge>)
    expect(screen.getByText('Success').className).toContain('bg-emerald-50')
  })

  it('aplica tom danger', () => {
    render(<Badge tone="danger">Danger</Badge>)
    expect(screen.getByText('Danger').className).toContain('bg-red-50')
  })

  it('aplica tom warning', () => {
    render(<Badge tone="warning">Warning</Badge>)
    expect(screen.getByText('Warning').className).toContain('bg-amber-50')
  })

  it('aplica tom info', () => {
    render(<Badge tone="info">Info</Badge>)
    expect(screen.getByText('Info').className).toContain('bg-sky-50')
  })
})
