export type ServiceCategoryStatus = 'ATIVO' | 'INATIVO'

export interface ServiceCategoryResponse {
  id: number
  name: string
  status: ServiceCategoryStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

export interface ServiceCategoryCreateRequest {
  name: string
  status?: ServiceCategoryStatus
}

export interface ServiceCategoryUpdateRequest {
  name?: string
  status?: ServiceCategoryStatus
}