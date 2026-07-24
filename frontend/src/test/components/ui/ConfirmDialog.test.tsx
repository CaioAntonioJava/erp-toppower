import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog.tsx'

describe('ConfirmDialog', () => {
  it('não renderiza quando open=false', () => {
    render(
      <ConfirmDialog open={false} title="Confirmar" onConfirm={vi.fn()} onClose={vi.fn()} />,
    )
    expect(screen.queryByText('Confirmar')).not.toBeInTheDocument()
  })

  it('renderiza título e botões quando open=true', () => {
    render(
      <ConfirmDialog open={true} title="Excluir?" onConfirm={vi.fn()} onClose={vi.fn()} />,
    )
    expect(screen.getByText('Excluir?')).toBeInTheDocument()
    expect(screen.getByText('Confirmar')).toBeInTheDocument()
    expect(screen.getByText('Cancelar')).toBeInTheDocument()
  })

  it('renderiza descrição quando informada', () => {
    render(
      <ConfirmDialog
        open={true}
        title="Excluir?"
        description="Esta ação não pode ser desfeita."
        onConfirm={vi.fn()}
        onClose={vi.fn()}
      />,
    )
    expect(screen.getByText('Esta ação não pode ser desfeita.')).toBeInTheDocument()
  })

  it('chama onConfirm ao clicar em Confirmar', () => {
    const onConfirm = vi.fn()
    render(
      <ConfirmDialog open={true} title="Tem certeza?" onConfirm={onConfirm} onClose={vi.fn()} />,
    )
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
    expect(onConfirm).toHaveBeenCalledOnce()
  })

  it('chama onClose ao clicar em Cancelar', () => {
    const onClose = vi.fn()
    render(
      <ConfirmDialog open={true} title="Tem certeza?" onConfirm={vi.fn()} onClose={onClose} />,
    )
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('usa variante danger quando confirmVariant=danger', () => {
    render(
      <ConfirmDialog
        open={true}
        title="Excluir?"
        confirmVariant="danger"
        onConfirm={vi.fn()}
        onClose={vi.fn()}
      />,
    )
    const confirmBtn = screen.getByRole('button', { name: 'Confirmar' })
    expect(confirmBtn.className).toContain('bg-red-600')
  })

  it('desabilita botões quando isLoading', () => {
    render(
      <ConfirmDialog
        open={true}
        title="Excluir?"
        isLoading={true}
        onConfirm={vi.fn()}
        onClose={vi.fn()}
      />,
    )
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
  })
})
