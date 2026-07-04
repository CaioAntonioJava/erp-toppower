/**
 * Mocks de propostas comerciais (Quotation).
 *
 * Apenas para desenvolvimento/teste manual. NÃO usar em produção.
 *
 * As propostas seed referenciam UUIDs determinísticos das outras listas
 * de mock (companies, customers, sellers, products) — todas compartilham
 * o padrão `00000000-0000-4000-8000-NNNNNNNNNNNN`, onde N é o índice
 * (1-based). Isso garante que o typeahead de clientes encontre os
 * registros referenciados pelas seeds.
 *
 * Total: 8 propostas cobrindo todos os status (ATIVA, EXPIRADA, CANCELADA,
 * CONVERTIDA), mistura de clientes PF/PJ, com e sem desconto.
 */

import type {
  DiscountType,
  FreightType,
  PaymentCondition,
  QuotationClientType,
  QuotationItemResponse,
  QuotationResponse,
  QuotationStatus,
} from '../types/quotation'
import { mockCompanies } from './companies.mock'
import { mockCustomers } from './customers.mock'
import { mockProducts } from './products.mock'
import { mockSellers } from './sellers.mock'
import { mockCarriers } from './carriers.mock'
import { SEED_AUTHOR, SEED_TIMESTAMP } from './helpers'

/** Número inicial — backend define 1500 como primeira proposta do sistema. */
const NUMBER_START = 1500

interface QItemSeed {
  /** Índice 0-based em `mockProducts`. */
  productIndex: number
  quantity: number
  /** Override do preço unitário (default = preço atual do produto). */
  unitPrice?: number
  discount?: { type: DiscountType; value: number }
}

interface QSeed {
  clientType: QuotationClientType
  /** Índice 0-based em `mockCustomers` (se CUSTOMER) ou `mockCompanies` (se COMPANY). */
  clientIndex: number
  /** Índice 0-based em `mockSellers`. */
  sellerIndex: number
  status: QuotationStatus
  issueDate: string
  attention?: string
  items: QItemSeed[]
  globalDiscount?: { type: DiscountType; value: number }
  validityDays?: number
  paymentCondition?: PaymentCondition
  notes?: string
  /** Índice 0-based em `mockCarriers`. Omitir = sem transportadora. */
  carrierIndex?: number
  /** Tipo de frete (CIF/FOB). Omitir = sem tipo. */
  freightType?: FreightType
  /** Valor do frete (manual). Omitir = sem frete. */
  freightValue?: number
  /** Margem de lucro aplicada sobre o total da proposta (em %). Default 0. */
  profitMargin?: number
}

const SEEDS: QSeed[] = [
  {
    clientType: 'CUSTOMER',
    clientIndex: 0, // Maria das Graças Silva
    sellerIndex: 0, // Carlos Eduardo Mendes
    status: 'ATIVA',
    issueDate: '2026-06-15',
    attention: 'Sr. Marcos',
    items: [
      { productIndex: 0, quantity: 50 }, // Cabo Flexível PP 2x2,5
      { productIndex: 6, quantity: 4 }, // Tomada 2P+T 20A
    ],
    globalDiscount: { type: 'PERCENT', value: 5 },
    validityDays: 15,
    paymentCondition: 'PIX',
    notes: 'Entrega em 5 dias úteis. Garantia de 1 ano.',
    carrierIndex: 0, // Correios SEDEX
    freightType: 'CIF',
    freightValue: 45.9,
  },
  {
    clientType: 'COMPANY',
    clientIndex: 0, // TopPower Energia
    sellerIndex: 1, // Renata Lopes
    status: 'ATIVA',
    issueDate: '2026-06-20',
    items: [
      { productIndex: 2, quantity: 6 }, // Cabo Rígido 750V 4mm² (rolo)
      { productIndex: 4, quantity: 12 }, // Disjuntor Monopolar 20A
      { productIndex: 5, quantity: 4 }, // Disjuntor Tripolar 63A
      { productIndex: 8, quantity: 2 }, // Quadro de Distribuição 12
    ],
    globalDiscount: { type: 'AMOUNT', value: 250 },
    validityDays: 30,
    paymentCondition: 'BOLETO_30_DIAS',
    notes: 'Instalação industrial — volumes fracionados conforme cronograma.',
    carrierIndex: 2, // JadLog
    freightType: 'CIF',
    freightValue: 58.0,
  },
  {
    clientType: 'CUSTOMER',
    clientIndex: 1, // João Carlos Pereira
    sellerIndex: 2, // Marcelo Augusto Reis
    status: 'ATIVA',
    issueDate: '2026-06-25',
    attention: 'Eng. João',
    items: [
      { productIndex: 1, quantity: 80 }, // Cabo Flexível PP 3x2,5
      { productIndex: 9, quantity: 12 }, // Eletroduto PVC Rígido 3/4"
    ],
    validityDays: 10,
    paymentCondition: 'A_VISTA_DINHEIRO',
  },
  {
    clientType: 'COMPANY',
    clientIndex: 3, // Iluminar Projetos
    sellerIndex: 0, // Carlos Eduardo Mendes
    status: 'EXPIRADA',
    issueDate: '2026-04-10',
    items: [
      { productIndex: 10, quantity: 100 }, // Lâmpada LED Bulbo 9W
      { productIndex: 7, quantity: 30 }, // Interruptor Simples 10A
    ],
    validityDays: 30,
    paymentCondition: 'PARCELAS_30_60',
    notes: 'Proposta expirada — cliente não respondeu no prazo.',
  },
  {
    clientType: 'CUSTOMER',
    clientIndex: 4, // Camila Rodrigues
    sellerIndex: 1, // Renata Lopes
    status: 'CONVERTIDA',
    issueDate: '2026-05-12',
    items: [
      { productIndex: 3, quantity: 2 }, // Cabo Coaxial RG6 (rolo 100m)
      { productIndex: 10, quantity: 20 }, // Lâmpada LED Bulbo 9W
    ],
    validityDays: 15,
    paymentCondition: 'PIX',
    notes: 'Convertida em pedido — ver OS #4521.',
  },
  {
    clientType: 'COMPANY',
    clientIndex: 6, // Condutor Brasileiro
    sellerIndex: 2, // Marcelo Augusto Reis
    status: 'CANCELADA',
    issueDate: '2026-05-28',
    items: [
      { productIndex: 11, quantity: 10 }, // Cabo de Cobre NU 50mm² (rolo)
    ],
    validityDays: 7,
    paymentCondition: 'BOLETO_28_DIAS',
    notes: 'Cancelada — cliente solicitou revisão de preço.',
  },
  {
    clientType: 'CUSTOMER',
    clientIndex: 6, // Fernanda Castro
    sellerIndex: 0, // Carlos Eduardo Mendes
    status: 'ATIVA',
    issueDate: '2026-06-30',
    attention: 'Sra. Fernanda',
    items: [
      { productIndex: 0, quantity: 30, discount: { type: 'PERCENT', value: 10 } },
      { productIndex: 4, quantity: 8 },
    ],
    globalDiscount: { type: 'PERCENT', value: 3 },
    validityDays: 20,
    paymentCondition: 'ENTRADA_MAIS_30_60_DIAS',
  },
  {
    clientType: 'COMPANY',
    clientIndex: 9, // Joule Materiais
    sellerIndex: 1, // Renata Lopes
    status: 'ATIVA',
    issueDate: '2026-07-01',
    items: [
      { productIndex: 5, quantity: 20 },
      { productIndex: 8, quantity: 5 },
      { productIndex: 9, quantity: 50 },
      { productIndex: 6, quantity: 30 },
    ],
    validityDays: 45,
    paymentCondition: 'FATURADO_45_DIAS',
    notes: 'Atendimento a obra no interior — frete CIF.',
    carrierIndex: 1, // Correios PAC
    freightType: 'FOB',
    freightValue: 32.5,
  },
]

function buildItem(seed: QItemSeed, itemIndex: number, quotationIndex: number): QuotationItemResponse {
  const product = mockProducts[seed.productIndex]
  const unitPrice = seed.unitPrice ?? product.price
  const lineSubtotal = unitPrice * seed.quantity
  let totalPrice = lineSubtotal
  if (seed.discount) {
    if (seed.discount.type === 'PERCENT') {
      totalPrice = Math.max(0, lineSubtotal - (lineSubtotal * seed.discount.value) / 100)
    } else {
      totalPrice = Math.max(0, lineSubtotal - seed.discount.value)
    }
  }
  return {
    uuid: `00000000-0000-4000-9000-${String(quotationIndex + 1).padStart(6, '0')}${String(itemIndex + 1).padStart(6, '0')}`,
    productUuid: product.uuid,
    quantity: seed.quantity,
    unitPrice,
    lineSubtotal,
    discountType: seed.discount?.type ?? null,
    discount: seed.discount?.value ?? null,
    totalPrice,
  }
}

function buildQuotation(seed: QSeed, index: number): QuotationResponse {
  const customerUuid =
    seed.clientType === 'CUSTOMER' ? mockCustomers[seed.clientIndex].uuid : null
  const companyUuid =
    seed.clientType === 'COMPANY' ? mockCompanies[seed.clientIndex].uuid : null
  const seller = mockSellers[seed.sellerIndex]

  const items = seed.items.map((it, i) => buildItem(it, i, index))

  const subtotal = items.reduce((sum, it) => sum + it.totalPrice, 0)
  let globalDiscountValue = 0
  if (seed.globalDiscount) {
    if (seed.globalDiscount.type === 'PERCENT') {
      globalDiscountValue = Math.min(
        subtotal,
        (subtotal * seed.globalDiscount.value) / 100,
      )
    } else {
      globalDiscountValue = Math.min(subtotal, seed.globalDiscount.value)
    }
  }
  // Frete somado após o desconto — nunca entra no desconto.
  const freight = seed.freightValue ?? 0
  // Margem de lucro aplicada por último, como multiplicação (1 + margin/100).
  const margin = seed.profitMargin ?? 0
  const total =
    (Math.max(0, subtotal - globalDiscountValue) + freight) * (1 + margin / 100)
  const totalQuantity = items.reduce((sum, it) => sum + it.quantity, 0)

  return {
    uuid: `00000000-0000-4000-8000-${String(index + 1).padStart(12, '0')}`,
    number: NUMBER_START + index,
    issueDate: seed.issueDate,
    customerUuid,
    companyUuid,
    clientType: seed.clientType,
    attention: seed.attention ?? null,
    sellerUuid: seller.uuid,
    items,
    discountType: seed.globalDiscount?.type ?? null,
    discount: seed.globalDiscount?.value ?? null,
    validityDays: seed.validityDays ?? null,
    paymentCondition: seed.paymentCondition ?? null,
    notes: seed.notes ?? null,
    status: seed.status,
    carrierUuid: seed.carrierIndex != null ? mockCarriers[seed.carrierIndex].uuid : null,
    freightType: seed.freightType ?? null,
    freightValue: seed.freightValue ?? null,
    profitMargin: margin,
    subtotal,
    total,
    totalQuantity,
    createdAt: SEED_TIMESTAMP,
    updatedAt: SEED_TIMESTAMP,
    createdBy: SEED_AUTHOR,
    updatedBy: null,
  }
}

export const mockQuotations: ReadonlyArray<QuotationResponse> = SEEDS.map(buildQuotation)