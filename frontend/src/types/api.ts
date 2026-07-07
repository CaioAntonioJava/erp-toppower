/** Tipos que espelham os DTOs do backend Spring Boot (ERP TopPower). */

export type Role = 'ROLE_ADMIN' | 'ROLE_MANAGER'

export type ProfileStatus = 'ATIVO' | 'INATIVO'

export type OrganizationStatus = 'ATIVO' | 'INATIVO'

/** Resumo de uma Organization (usado no seletor e na resposta de login). */
export interface OrganizationSummary {
  uuid: string
  corporateName: string
  tradeName: string
  cnpj: string
  status: OrganizationStatus
  /** Papel do usuário nesta Organization (quando aplicável). */
  role?: Role | null
  /** Indica se esta é a Organization default do usuário. */
  isDefault?: boolean
}

/** Usuário autenticado (retornado em /auth/login e /me). Espelha LoginResponse.AuthenticatedUser. */
export interface AuthenticatedUser {
  uuid: string
  email: string
  role: Role
}

/** Resposta de POST /api/v1/auth/login. */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthenticatedUser
  /** Organizations acessíveis ao usuário (para o seletor). */
  organizations: OrganizationSummary[]
  /** UUID da Organization default (pré-selecionada após o login). Pode ser null. */
  defaultOrganizationId: string | null
}

/** Corpo de POST /api/v1/auth/login. */
export interface LoginRequest {
  email: string
  password: string
}

/** Corpo de POST /api/v1/users (cadastro de usuário pelo admin). */
export interface RegisterRequest {
  email: string
  password: string
  passwordConfirmation: string
}

/** Corpo de PATCH /api/v1/users/{id}/password. */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** Corpo de PATCH /api/v1/users/{id}/reset-password (admin redefine senha). */
export interface ResetPasswordRequest {
  newPassword: string
}

/** Resposta de usuário (cadastro). Espelha br.com.toppower...user.dto.UserResponse. */
export interface UserResponse {
  uuid: string
  email: string
  role: Role
}

/** Resposta de perfil. Espelha br.com.toppower...profile.dto.ProfileResponse. */
export interface ProfileResponse {
  uuid: string
  name: string
  email: string
  phone: string
  cpf: string
  status: ProfileStatus
  userId: string
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/profiles. */
export interface ProfileCreateRequest {
  name: string
  email: string
  phone: string
  cpf: string
  status?: ProfileStatus
}

/** Corpo de PATCH /api/v1/profiles/{id}. Campos opcionais. */
export interface ProfileUpdateRequest {
  name?: string
  email?: string
  phone?: string
  cpf?: string
  status?: ProfileStatus
}

/** Estrutura de erro padronizada pelo GlobalExceptionHandler do backend. */
export interface ApiError {
  status: number
  message: string
  timestamp: string
  /** Presente apenas em erros de validação (400). Mapa campo -> mensagem. */
  fieldErrors?: Record<string, string>
}

/** Resposta paginada genérica. Espelha br.com.toppower...common.dto.PagedResponse. */
export interface PagedResponse<T> {
  content: T[]
  /** 0-indexed. */
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
