/**
 * Mocks de clientes (pessoas físicas).
 *
 * Apenas para desenvolvimento/teste manual. NÃO usar em produção.
 *
 * Total: 12 clientes — mistura de ATIVOS e INATIVOS, CPFs válidos.
 */

import type { CustomerResponse } from '../types/customer'
import {
  SEED_AUTHOR,
  SEED_TIMESTAMP,
  digitsCpf,
  formatCpf,
  formatPhone,
  formatZip,
} from './helpers'

interface CustomerSeed {
  name: string
  email: string
  /** 9 dígitos — os 2 verificadores são calculados. */
  cpfBase: readonly number[]
  phoneBase: readonly number[]
  street: string
  number: string
  neighborhood: string
  city: string
  state: string
  zipBase: readonly number[]
  status: 'ATIVO' | 'INATIVO'
}

const RAW: CustomerSeed[] = [
  {
    name: 'Maria das Graças Silva',
    email: 'maria.silva@example.com',
    cpfBase: [1, 1, 1, 2, 2, 2, 3, 3, 3],
    phoneBase: [1, 1, 9, 9, 8, 8, 7, 7, 6, 5, 4],
    street: 'Rua das Acácias',
    number: '120',
    neighborhood: 'Jardim América',
    city: 'São Paulo',
    state: 'SP',
    zipBase: [0, 1, 2, 3, 0, 4, 0, 1],
    status: 'ATIVO',
  },
  {
    name: 'João Carlos Pereira',
    email: 'joao.pereira@example.com',
    cpfBase: [1, 2, 3, 4, 5, 6, 7, 8, 9],
    phoneBase: [2, 1, 9, 9, 7, 7, 1, 2, 3, 4, 5],
    street: 'Av. Brasil',
    number: '540',
    neighborhood: 'Funcionários',
    city: 'Belo Horizonte',
    state: 'MG',
    zipBase: [3, 0, 1, 1, 0, 0, 0, 2],
    status: 'ATIVO',
  },
  {
    name: 'Ana Paula Fernandes',
    email: 'ana.fernandes@example.com',
    cpfBase: [2, 3, 4, 5, 6, 7, 8, 9, 0],
    phoneBase: [3, 1, 9, 9, 5, 5, 9, 8, 7, 6, 5],
    street: 'Rua Voluntários da Pátria',
    number: '880',
    neighborhood: 'Centro',
    city: 'Porto Alegre',
    state: 'RS',
    zipBase: [9, 0, 0, 2, 0, 0, 0, 3],
    status: 'ATIVO',
  },
  {
    name: 'Pedro Henrique Souza',
    email: 'pedro.souza@example.com',
    cpfBase: [3, 4, 5, 6, 7, 8, 9, 0, 1],
    phoneBase: [4, 1, 9, 9, 1, 1, 4, 4, 4, 4, 4],
    street: 'Rua XV de Novembro',
    number: '200',
    neighborhood: 'Centro',
    city: 'Curitiba',
    state: 'PR',
    zipBase: [8, 0, 0, 3, 0, 0, 0, 4],
    status: 'INATIVO',
  },
  {
    name: 'Camila Rodrigues de Oliveira',
    email: 'camila.oliveira@example.com',
    cpfBase: [4, 5, 6, 7, 8, 9, 0, 1, 2],
    phoneBase: [5, 1, 9, 9, 9, 9, 0, 1, 2, 3, 4],
    street: 'Av. Paulista',
    number: '2300',
    neighborhood: 'Bela Vista',
    city: 'São Paulo',
    state: 'SP',
    zipBase: [0, 1, 3, 1, 0, 0, 0, 5],
    status: 'ATIVO',
  },
  {
    name: 'Lucas Almeida Martins',
    email: 'lucas.martins@example.com',
    cpfBase: [5, 6, 7, 8, 9, 0, 1, 2, 3],
    phoneBase: [6, 1, 9, 9, 3, 3, 5, 6, 7, 8, 9],
    street: 'Rua dos Pinheiros',
    number: '450',
    neighborhood: 'Pinheiros',
    city: 'São Paulo',
    state: 'SP',
    zipBase: [0, 5, 4, 2, 0, 0, 0, 6],
    status: 'ATIVO',
  },
  {
    name: 'Fernanda Castro Lima',
    email: 'fernanda.lima@example.com',
    cpfBase: [6, 7, 8, 9, 0, 1, 2, 3, 4],
    phoneBase: [7, 1, 9, 9, 8, 8, 8, 8, 7, 6, 5],
    street: 'Av. Agamenon Magalhães',
    number: '1200',
    neighborhood: 'Boa Viagem',
    city: 'Recife',
    state: 'PE',
    zipBase: [5, 0, 0, 7, 0, 0, 0, 7],
    status: 'ATIVO',
  },
  {
    name: 'Rafael Mendes dos Santos',
    email: 'rafael.santos@example.com',
    cpfBase: [7, 8, 9, 0, 1, 2, 3, 4, 5],
    phoneBase: [8, 1, 9, 9, 6, 6, 1, 2, 3, 4, 5],
    street: 'Rua da Praia',
    number: '33',
    neighborhood: 'Centro Histórico',
    city: 'Porto Alegre',
    state: 'RS',
    zipBase: [9, 0, 0, 1, 0, 0, 0, 8],
    status: 'ATIVO',
  },
  {
    name: 'Juliana Ribeiro da Costa',
    email: 'juliana.costa@example.com',
    cpfBase: [8, 9, 0, 1, 2, 3, 4, 5, 6],
    phoneBase: [9, 1, 9, 9, 7, 7, 0, 0, 1, 1, 1],
    street: 'Av. Goiás',
    number: '800',
    neighborhood: 'Setor Central',
    city: 'Goiânia',
    state: 'GO',
    zipBase: [7, 4, 0, 2, 0, 0, 0, 9],
    status: 'INATIVO',
  },
  {
    name: 'Bruno Henrique Barbosa',
    email: 'bruno.barbosa@example.com',
    cpfBase: [9, 0, 1, 2, 3, 4, 5, 6, 7],
    phoneBase: [1, 2, 9, 9, 5, 5, 2, 2, 2, 2, 2],
    street: 'Av. Sete de Setembro',
    number: '1100',
    neighborhood: 'Centro',
    city: 'Salvador',
    state: 'BA',
    zipBase: [4, 0, 0, 1, 0, 0, 0, 1],
    status: 'ATIVO',
  },
  {
    name: 'Patrícia Nogueira Vieira',
    email: 'patricia.vieira@example.com',
    cpfBase: [1, 3, 5, 7, 9, 1, 3, 5, 7],
    phoneBase: [2, 2, 9, 9, 4, 4, 9, 9, 9, 9, 9],
    street: 'Rua Padre Anchieta',
    number: '2500',
    neighborhood: 'Bigorrilho',
    city: 'Curitiba',
    state: 'PR',
    zipBase: [8, 0, 7, 3, 0, 0, 0, 2],
    status: 'ATIVO',
  },
  {
    name: 'Gustavo Lima de Araújo',
    email: 'gustavo.araujo@example.com',
    cpfBase: [2, 4, 6, 8, 0, 2, 4, 6, 8],
    phoneBase: [3, 2, 9, 9, 1, 1, 7, 7, 7, 7, 7],
    street: 'Av. Dom Luís',
    number: '700',
    neighborhood: 'Aldeota',
    city: 'Fortaleza',
    state: 'CE',
    zipBase: [6, 0, 1, 1, 0, 0, 0, 3],
    status: 'ATIVO',
  },
]

function build(seed: CustomerSeed, index: number): CustomerResponse {
  return {
    uuid: `00000000-0000-4000-8000-${String(index + 1).padStart(12, '0')}`,
    name: seed.name,
    email: seed.email,
    phone: formatPhone(seed.phoneBase.join('')),
    cpf: formatCpf(digitsCpf(seed.cpfBase)),
    code: `CLI${String(index + 1).padStart(6, '0')}`,
    address: {
      street: seed.street,
      number: seed.number,
      neighborhood: seed.neighborhood,
      city: seed.city,
      state: seed.state,
      zipCode: formatZip(seed.zipBase.join('')),
    },
    status: seed.status,
    createdAt: SEED_TIMESTAMP,
    updatedAt: SEED_TIMESTAMP,
    createdBy: SEED_AUTHOR,
    updatedBy: null,
  }
}

export const mockCustomers: ReadonlyArray<CustomerResponse> = RAW.map(build)