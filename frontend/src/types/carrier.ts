import type { RegistrationStatus } from './registration'

export type { RegistrationStatus }

export interface CarrierResponse {
  id: number
  name: string
  status: RegistrationStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

export interface CarrierCreateRequest {
  name: string
  status?: RegistrationStatus
}

export interface CarrierUpdateRequest {
  name?: string
  status?: RegistrationStatus
}

export interface CarrierFilters {
  query?: string
  status?: RegistrationStatus | 'ALL'
  page?: number
  size?: number
}