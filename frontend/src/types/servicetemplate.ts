export type ServiceCategory = 'EXECUÇÃO_SPDA'

export const SERVICE_CATEGORIES: { value: ServiceCategory; label: string }[] = [
  { value: 'EXECUÇÃO_SPDA', label: 'EXECUÇÃO SPDA' },
]

export interface ServiceTemplateResponse {
  id: number
  name: string
  description: string | null
  category: ServiceCategory
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

export interface ServiceTemplateCreateRequest {
  name: string
  description?: string | null
  category: ServiceCategory
}

export interface ServiceTemplateUpdateRequest {
  name?: string
  description?: string | null
  category?: ServiceCategory
}

export interface ServiceTemplateFilters {
  query?: string
  page?: number
  size?: number
}
