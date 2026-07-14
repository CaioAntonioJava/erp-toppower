export interface ServiceTemplateResponse {
  id: number
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

export interface ServiceTemplateCreateRequest {
  name: string
  description?: string | null
}

export interface ServiceTemplateUpdateRequest {
  name?: string
  description?: string | null
}

export interface ServiceTemplateFilters {
  query?: string
  page?: number
  size?: number
}
