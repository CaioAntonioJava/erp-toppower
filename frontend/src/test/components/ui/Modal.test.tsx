import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Modal } from '../../../components/ui/Modal.tsx'

describe('Modal', () => {
  it('não renderiza quando open=false', () => {
    render(
      <Modal open={false} title="Título" onClose={vi.fn()}>
        Conteúdo
      </Modal>,
    )
    expect(screen.queryByText('Título')).not.toBeInTheDocument()
  })

  it('renderiza título e conteúdo quando open=true', () => {
    render(
      <Modal open={true} title="Título do Modal" onClose={vi.fn()}>
        Conteúdo do modal
      </Modal>,
    )
    expect(screen.getByText('Título do Modal')).toBeInTheDocument()
    expect(screen.getByText('Conteúdo do modal')).toBeInTheDocument()
  })

  it('renderiza descrição quando informada', () => {
    render(
      <Modal open={true} title="Título" description="Descrição" onClose={vi.fn()}>
        Conteúdo
      </Modal>,
    )
    expect(screen.getByText('Descrição')).toBeInTheDocument()
  })

  it('chama onClose ao clicar no botão fechar', () => {
    const onClose = vi.fn()
    render(
      <Modal open={true} title="Título" onClose={onClose}>
        Conteúdo
      </Modal>,
    )
    fireEvent.click(screen.getByLabelText('Fechar'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('renderiza footer quando informado', () => {
    render(
      <Modal open={true} title="Título" onClose={vi.fn()} footer={<button>Salvar</button>}>
        Conteúdo
      </Modal>,
    )
    expect(screen.getByText('Salvar')).toBeInTheDocument()
  })
})
