import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Input } from '../../../components/ui/Input.tsx'

describe('Input', () => {
  it('renderiza input com label', () => {
    render(<Input label="Nome" />)
    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
  })

  it('renderiza asterisco vermelho quando required', () => {
    render(<Input label="Email" required />)
    expect(screen.getByText('*')).toBeInTheDocument()
  })

  it('exibe mensagem de erro', () => {
    render(<Input label="Campo" error="Campo obrigatório" />)
    expect(screen.getByText('Campo obrigatório')).toBeInTheDocument()
  })

  it('exibe hint quando não há erro', () => {
    render(<Input label="Campo" hint="Informe seu nome" />)
    expect(screen.getByText('Informe seu nome')).toBeInTheDocument()
  })

  it('não exibe hint quando há erro', () => {
    render(<Input label="Campo" error="Erro" hint="Hint" />)
    expect(screen.queryByText('Hint')).not.toBeInTheDocument()
  })

  it('renderiza input com placeholder', () => {
    render(<Input placeholder="Digite aqui" />)
    expect(screen.getByPlaceholderText('Digite aqui')).toBeInTheDocument()
  })

  it('renderiza botão de mostrar/ocultar senha para type password', () => {
    render(<Input type="password" label="Senha" />)
    expect(screen.getByLabelText('Mostrar senha')).toBeInTheDocument()
  })

  it('aplica aria-invalid quando tem erro', () => {
    render(<Input label="Campo" error="Erro" />)
    expect(screen.getByLabelText('Campo')).toHaveAttribute('aria-invalid', 'true')
  })
})
