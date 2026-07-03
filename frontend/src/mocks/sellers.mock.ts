/**
 * Mocks de vendedores (pessoas físicas com comissão).
 *
 * Apenas para desenvolvimento/teste manual. NÃO usar em produção.
 *
 * Total: 12 vendedores — mistura de ATIVOS e INATIVOS, comissões de 1% a 10%.
 */

import type { SellerResponse } from '../types/seller'
import {
  SEED_AUTHOR,
  SEED_TIMESTAMP,
  digitsCpf,
  formatCpf,
  formatPhone,
} from './helpers'

interface SellerSeed {
  name: string
  email: string
  /** 9 dígitos — os 2 verificadores são calculados. */
  cpfBase: readonly number[]
  phoneBase: readonly number[]
  commissionRate: number | null
  status: 'ATIVO' | 'INATIVO'
}

const RAW: SellerSeed[] = [
  {
    name: 'Carlos Eduardo Mendes',
    email: 'carlos.mendes@toppower.local',
    cpfBase: [1, 1, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [1, 1, 9, 8, 1, 2, 3, 4, 5, 6, 7],
    commissionRate: 3.5,
    status: 'ATIVO',
  },
  {
    name: 'Renata Lopes Carvalho',
    email: 'renata.carvalho@toppower.local',
    cpfBase: [1, 2, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [1, 1, 9, 9, 5, 5, 5, 5, 5, 5, 5],
    commissionRate: 2.0,
    status: 'ATIVO',
  },
  {
    name: 'Marcelo Augusto Reis',
    email: 'marcelo.reis@toppower.local',
    cpfBase: [1, 3, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [2, 1, 9, 8, 1, 2, 1, 1, 1, 1, 1],
    commissionRate: 5.0,
    status: 'ATIVO',
  },
  {
    name: 'Tatiana Vieira Borges',
    email: 'tatiana.borges@toppower.local',
    cpfBase: [1, 4, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [3, 1, 9, 7, 7, 7, 8, 8, 8, 8, 8],
    commissionRate: 4.0,
    status: 'ATIVO',
  },
  {
    name: 'Diego Soares de Freitas',
    email: 'diego.freitas@toppower.local',
    cpfBase: [1, 5, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [4, 1, 9, 6, 6, 6, 9, 9, 9, 9, 9],
    commissionRate: 1.5,
    status: 'INATIVO',
  },
  {
    name: 'Larissa Gonçalves Pinto',
    email: 'larissa.pinto@toppower.local',
    cpfBase: [1, 6, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [5, 1, 9, 5, 5, 5, 0, 1, 2, 3, 4],
    commissionRate: 6.5,
    status: 'ATIVO',
  },
  {
    name: 'Fábio Augusto Nogueira',
    email: 'fabio.nogueira@toppower.local',
    cpfBase: [1, 7, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [6, 1, 9, 4, 4, 4, 5, 6, 7, 8, 9],
    commissionRate: 2.5,
    status: 'ATIVO',
  },
  {
    name: 'Vanessa Duarte Moreira',
    email: 'vanessa.moreira@toppower.local',
    cpfBase: [1, 8, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [7, 1, 9, 3, 3, 3, 0, 0, 0, 0, 1],
    commissionRate: 3.0,
    status: 'ATIVO',
  },
  {
    name: 'Eduardo Salles Teixeira',
    email: 'eduardo.teixeira@toppower.local',
    cpfBase: [1, 9, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [8, 1, 9, 2, 2, 2, 9, 9, 8, 8, 8],
    commissionRate: 7.0,
    status: 'ATIVO',
  },
  {
    name: 'Beatriz Amaral Sales',
    email: 'beatriz.sales@toppower.local',
    cpfBase: [2, 0, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [9, 1, 9, 1, 1, 1, 7, 7, 7, 7, 6],
    commissionRate: null,
    status: 'ATIVO',
  },
  {
    name: 'Henrique Pacheco Brandão',
    email: 'henrique.brandao@toppower.local',
    cpfBase: [2, 1, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [1, 2, 9, 9, 9, 9, 6, 6, 6, 6, 5],
    commissionRate: 4.5,
    status: 'INATIVO',
  },
  {
    name: 'Isabela Cordeiro Maia',
    email: 'isabela.maia@toppower.local',
    cpfBase: [2, 2, 2, 3, 3, 4, 4, 5, 5],
    phoneBase: [2, 2, 9, 8, 8, 8, 4, 4, 4, 4, 3],
    commissionRate: 10.0,
    status: 'ATIVO',
  },
]

function build(seed: SellerSeed, index: number): SellerResponse {
  return {
    uuid: `00000000-0000-4000-8000-${String(index + 1).padStart(12, '0')}`,
    name: seed.name,
    email: seed.email,
    phone: formatPhone(seed.phoneBase.join('')),
    cpf: formatCpf(digitsCpf(seed.cpfBase)),
    commissionRate: seed.commissionRate,
    status: seed.status,
    createdAt: SEED_TIMESTAMP,
    updatedAt: SEED_TIMESTAMP,
    createdBy: SEED_AUTHOR,
    updatedBy: null,
  }
}

export const mockSellers: ReadonlyArray<SellerResponse> = RAW.map(build)