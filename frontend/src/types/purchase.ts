/** Tipos do módulo de importação de NF-e. Espelham os DTOs do backend. */

export type ItemStatus = 'NOVO' | 'EXISTENTE' | 'DIVERGENTE'

export interface NfeInstallmentData {
  number: string
  dueDate: string
  amount: number
}

export interface NfeSupplierData {
  existing: boolean
  id: number | null
  taxId: string
  legalName: string
  tradeName: string | null
  stateRegistration: string | null
  municipalRegistration: string | null
  street: string | null
  number: string | null
  complement: string | null
  neighborhood: string | null
  city: string | null
  state: string | null
  zipCode: string | null
}

export interface NfeItemData {
  status: ItemStatus
  productId: number | null
  code: string | null
  codigoBarras: string | null
  name: string
  ncm: string
  cest: string | null
  unit: string
  quantity: number
  unitValue: number
  totalValue: number
  origem: string | null
  pesoLiquido: number | null
  pesoBruto: number | null
}

export interface NfePayableData {
  value: number
  issueDate: string
  description: string
  invoiceNumber: string
  accessKey: string | null
  installments: NfeInstallmentData[]
}

export interface NfePreviewResponse {
  xmlBase64: string
  supplier: NfeSupplierData
  items: NfeItemData[]
  payable: NfePayableData
}

export interface NfeConfirmRequest {
  xmlBase64: string
}

export interface NfeConfirmResponse {
  supplierId: number
  supplierCreated: boolean
  createdProductIds: number[]
  existingProductIds: number[]
  payableId: number
  invoiceNumber: string
}