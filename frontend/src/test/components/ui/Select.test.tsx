import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Select } from '../../../components/ui/Select.tsx'

const options = [
  { value: '1', label: 'Opção 1' },
  { value: '2', label: 'Opção 2' },
  { value: '3', label: 'Opção 3' },
] as const

describe('Select', () => {
  it('renderiza select com label', () => {
    render(<Select label="Escolha" options={options} />)
    expect(screen.getByLabelText('Escolha')).toBeInTheDocument()
  })

  it('renderiza todas as opções', () => {
    render(<Select label="Escolha" options={options} />)
    expect(screen.getByText('Opção 1')).toBeInTheDocument()
    expect(screen.getByText('Opção 2')).toBeInTheDocument()
    expect(screen.getByText('Opção 3')).toBeInTheDocument()
  })

  it('renderiza placeholder quando informado', () => {
    render(<Select label="Escolha" options={options} placeholder="Selecione..." />)
    expect(screen.getByText('Selecione...')).toBeInTheDocument()
  })

  it('exibe mensagem de erro', () => {
    render(<Select label="Escolha" options={options} error="Campo obrigatório" />)
    expect(screen.getByText('Campo obrigatório')).toBeInTheDocument()
  })

  it('exibe hint quando não há erro', () => {
    render(<Select label="Escolha" options={options} hint="Escolha uma opção" />)
    expect(screen.getByText('Escolha uma opção')).toBeInTheDocument()
  })

  it('aplica aria-invalid quando tem erro', () => {
    render(<Select label="Escolha" options={options} error="Erro" />)
    expect(screen.getByLabelText('Escolha')).toHaveAttribute('aria-invalid', 'true')
  })
})
