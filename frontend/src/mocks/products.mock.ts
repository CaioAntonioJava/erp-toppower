/**
 * Mocks de produtos.
 *
 * Apenas para desenvolvimento/teste manual. NÃO usar em produção.
 *
 * Produtos coerentes com o domínio da TopPower (materiais elétricos):
 * cabos, disjuntores, tomadas, eletrodutos, etc. Total: 12 produtos.
 *
 * Cobre os 3 `UnitType`: UNIDADE, METROS e BOBINA. Mistura de
 * ATIVOS/INATIVOS e com/sem SKU (campo `code`).
 */

import type { ProductResponse, UnitType } from '../types/product'
import { SEED_AUTHOR, SEED_TIMESTAMP } from './helpers'

interface ProductSeed {
  name: string
  /** Quando `null`, o produto é cadastrado sem SKU (campo opcional no backend). */
  code: string | null
  unitType: UnitType
  price: number
  stockQuantity: number
  status: 'ATIVO' | 'INATIVO'
}

const RAW: ProductSeed[] = [
  {
    name: 'Cabo Flexível PP 2x2,5 mm² 750V',
    code: 'CAB-PP-2X2.5',
    unitType: 'METROS',
    price: 6.9,
    stockQuantity: 1500,
    status: 'ATIVO',
  },
  {
    name: 'Cabo Flexível PP 3x2,5 mm² 750V',
    code: 'CAB-PP-3X2.5',
    unitType: 'METROS',
    price: 9.5,
    stockQuantity: 980,
    status: 'ATIVO',
  },
  {
    name: 'Cabo Rígido 750V 4 mm² (rolo 100m)',
    code: 'CAB-RIG-4-100',
    unitType: 'BOBINA',
    price: 489.9,
    stockQuantity: 24,
    status: 'ATIVO',
  },
  {
    name: 'Cabo Coaxial RG6 75 Ohm (rolo 100m)',
    code: 'CAB-COX-RG6-100',
    unitType: 'BOBINA',
    price: 219.5,
    stockQuantity: 12,
    status: 'ATIVO',
  },
  {
    name: 'Disjuntor Monopolar 20A Curva C',
    code: 'DISJ-MON-20C',
    unitType: 'UNIDADE',
    price: 12.9,
    stockQuantity: 320,
    status: 'ATIVO',
  },
  {
    name: 'Disjuntor Tripolar 63A Curva C',
    code: 'DISJ-TRI-63C',
    unitType: 'UNIDADE',
    price: 119.9,
    stockQuantity: 75,
    status: 'ATIVO',
  },
  {
    name: 'Tomada 2P+T 20A Branca',
    code: 'TOM-2PT-20-BR',
    unitType: 'UNIDADE',
    price: 8.5,
    stockQuantity: 540,
    status: 'ATIVO',
  },
  {
    name: 'Interruptor Simples 10A Branco',
    code: 'INT-SIM-10-BR',
    unitType: 'UNIDADE',
    price: 6.2,
    stockQuantity: 610,
    status: 'ATIVO',
  },
  {
    name: 'Quadro de Distribuição 12 Disjuntores Sobrepor',
    code: 'QD-12-SOB',
    unitType: 'UNIDADE',
    price: 145.0,
    stockQuantity: 38,
    status: 'ATIVO',
  },
  {
    name: 'Eletroduto PVC Rígido 3/4" (barra 3m)',
    code: 'ELE-PVC-3/4-3M',
    unitType: 'UNIDADE',
    price: 18.9,
    stockQuantity: 220,
    status: 'ATIVO',
  },
  {
    name: 'Lâmpada LED Bulbo 9W 6500K Bivolt',
    code: null,
    unitType: 'UNIDADE',
    price: 11.9,
    stockQuantity: 480,
    status: 'ATIVO',
  },
  {
    name: 'Cabo de Cobre NU 50 mm² (rolo 50m)',
    code: 'CAB-NU-50-50',
    unitType: 'BOBINA',
    price: 1890.0,
    stockQuantity: 4,
    status: 'INATIVO',
  },
]

function build(seed: ProductSeed, index: number): ProductResponse {
  return {
    uuid: `00000000-0000-4000-8000-${String(index + 1).padStart(12, '0')}`,
    name: seed.name,
    code: seed.code,
    unitType: seed.unitType,
    status: seed.status,
    price: seed.price,
    stockQuantity: seed.stockQuantity,
    createdAt: SEED_TIMESTAMP,
    updatedAt: SEED_TIMESTAMP,
    createdBy: SEED_AUTHOR,
    updatedBy: null,
  }
}

export const mockProducts: ReadonlyArray<ProductResponse> = RAW.map(build)