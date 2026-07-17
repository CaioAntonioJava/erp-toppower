export type ServiceCategory = 'EXECUÇÃO_SPDA'

export const SERVICE_CATEGORIES: { value: ServiceCategory; label: string }[] = [
  { value: 'EXECUÇÃO_SPDA', label: 'EXECUÇÃO SPDA' },
]

export interface ServiceTemplateResponse {
  id: number
  description: string | null
  category: ServiceCategory
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

export interface ServiceTemplateCreateRequest {
  description?: string | null
  category: ServiceCategory
}

export interface ServiceTemplateUpdateRequest {
  description?: string | null
  category?: ServiceCategory
}

export interface ServiceTemplateFilters {
  page?: number
  size?: number
}
