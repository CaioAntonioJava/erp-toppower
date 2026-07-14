/** Tipos que espelham os DTOs do backend Spring Boot (ERP TopPower). */

export type Role = 'ROLE_ADMIN' | 'ROLE_MANAGER'

export type ProfileStatus = 'ATIVO' | 'INATIVO'

export type OrganizationStatus = 'ATIVO' | 'INATIVO'

/** Resumo de uma Organization (usado no seletor e na resposta de login). */
export interface OrganizationSummary {
  id: number
  corporateName: string
  tradeName: string
  cnpj: string
  /** URL pública do logo (ex.: '/logos/<id>.png'). Opcional. */
  logoUrl?: string | null
  status: OrganizationStatus
  /**
   * Prefixo do código das Propostas Técnicas emitidas por esta Organization
   * (ex.: 'PT' para Top Power Engenharia, 'PL' para Top Power Materiais).
   * A sigla completa fica `<proposalPrefix>-<seq 3 dígitos>-<ano>`.
   */
  proposalPrefix: string
  /**
   * Prefixo do código dos Contratos emitidos por esta Organization
   * (ex.: 'CT' para Top Power Engenharia, 'CL' para Top Power Materiais).
   * A sigla completa fica `<contractPrefix>-<seq 3 dígitos>-<ano>`.
   */
  contractPrefix: string
  /**
   * Texto HTML padrão pré-preenchido na descrição de novos contratos.
   * Opcional — quando nulo ou vazio, a descrição inicia em branco.
   */
  contractDefaultDescription?: string | null
  /** Papel do usuário nesta Organization (quando aplicável). */
  role?: Role | null
  /** Indica se esta é a Organization default do usuário. */
  isDefault?: boolean
}

/**
 * Representação completa de uma Organization (espelha
 * {@code OrganizationResponse} do backend). Inclui endereço e demais
 * campos necessários para alimentar o cabeçalho dos PDFs sem precisar
 * de uma nova chamada de API.
 */
export interface OrganizationResponse {
  id: number
  corporateName: string
  tradeName: string
  cnpj: string
  stateRegistration?: string | null
  municipalRegistration?: string | null
  phone?: string | null
  email?: string | null
  zipCode?: string | null
  street?: string | null
  number?: string | null
  district?: string | null
  city?: string | null
  state?: string | null
  complement?: string | null
  logoUrl?: string | null
  status: OrganizationStatus
  proposalPrefix: string
  contractPrefix: string
  /** Texto HTML padrão pré-preenchido na descrição de novos contratos. */
  contractDefaultDescription?: string | null
  createdAt: string
  updatedAt: string
}

/**
 * Corpo de PATCH /api/v1/organizations/{id}. Todos os campos opcionais;
 * apenas os enviados são atualizados (PATCH semântico).
 */
export interface OrganizationUpdateRequest {
  corporateName?: string
  tradeName?: string
  stateRegistration?: string
  municipalRegistration?: string
  phone?: string
  email?: string
  zipCode?: string
  street?: string
  number?: string
  district?: string
  city?: string
  state?: string
  complement?: string
  logoUrl?: string
  status?: OrganizationStatus
  proposalPrefix?: string
  contractPrefix?: string
  /** Texto HTML padrão pré-preenchido na descrição de novos contratos. */
  contractDefaultDescription?: string | null
}

/** Usuário autenticado (retornado em /auth/login e /me). Espelha LoginResponse.AuthenticatedUser. */
export interface AuthenticatedUser {
  id: number
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
  /** ID da Organization default (pré-selecionada após o login). Pode ser null. */
  defaultOrganizationId: number | null
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
  id: number
  email: string
  role: Role
}

/** Corpo de POST /api/v1/user-organizations (vincular usuário a Organization). */
export interface UserOrganizationAssignRequest {
  userId: number
  organizationId: number
  /** Apenas ROLE_MANAGER no fluxo de criação; ADMIN não se auto-vincula. */
  role: Role
  isDefault?: boolean
}

/** Resposta de vínculos usuário↔Organization. Espelha UserOrganizationResponse (backend). */
export interface UserOrganizationResponse {
  id: number
  userId: number
  userEmail: string
  organizationId: number
  organizationCorporateName: string
  role: Role
  isDefault: boolean
  createdAt: string
}

/** Resposta de perfil. Espelha br.com.toppower...profile.dto.ProfileResponse. */
export interface ProfileResponse {
  id: number
  name: string
  email: string
  phone: string
  cpf: string
  status: ProfileStatus
  userId: number
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
