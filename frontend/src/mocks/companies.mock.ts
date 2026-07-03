/**
 * Mocks de empresas (pessoas jurídicas).
 *
 * Apenas para desenvolvimento/teste manual. NÃO usar em produção.
 *
 * Cada entrada espelha exatamente o tipo `CompanyResponse` retornado
 * pelo backend, então pode ser injetada em testes, Storybook ou em
 * qualquer camada que aceite a resposta da API.
 *
 * Total: 12 empresas — mistura de ATIVAS e INATIVAS, com e sem IE.
 */

import type { CompanyResponse } from '../types/company'
import {
  SEED_AUTHOR,
  SEED_TIMESTAMP,
  formatCnpj,
  formatPhone,
  formatZip,
  makeCnpj,
} from './helpers'

interface CompanySeed {
  legalName: string
  tradeName: string | null
  /** 12 dígitos — os 2 verificadores são calculados. */
  cnpjBase: readonly number[]
  stateRegistration: string | null
  stateRegistrationExempt: boolean
  municipalRegistration: string | null
  street: string
  number: string
  neighborhood: string
  city: string
  state: string
  zipBase: readonly number[]
  phoneBase: readonly number[]
  status: 'ATIVO' | 'INATIVO'
  createdBy: string | null
}

const RAW: CompanySeed[] = [
  {
    legalName: 'TopPower Energia e Automação Ltda.',
    tradeName: 'TopPower',
    cnpjBase: [1, 1, 2, 2, 3, 3, 0, 0, 0, 1, 2, 3],
    stateRegistration: '110.042.490.117',
    stateRegistrationExempt: false,
    municipalRegistration: '6.123.456-7',
    street: 'Av. Paulista',
    number: '1000',
    neighborhood: 'Bela Vista',
    city: 'São Paulo',
    state: 'SP',
    zipBase: [0, 1, 3, 1, 0, 5, 0, 0],
    phoneBase: [1, 1, 3, 5, 5, 5, 1, 2, 3, 4, 5],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Volta Nacional Materiais Elétricos S.A.',
    tradeName: 'Volta Nacional',
    cnpjBase: [2, 2, 3, 3, 4, 4, 0, 0, 0, 1, 2, 4],
    stateRegistration: '115.387.221.008',
    stateRegistrationExempt: false,
    municipalRegistration: '5.987.654-3',
    street: 'Rua das Indústrias',
    number: '250',
    neighborhood: 'Distrito Industrial',
    city: 'Curitiba',
    state: 'PR',
    zipBase: [8, 1, 2, 3, 0, 0, 0, 1],
    phoneBase: [4, 1, 3, 3, 3, 3, 1, 2, 3, 4, 5],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Cabos do Sul Distribuidora Ltda.',
    tradeName: 'Cabos do Sul',
    cnpjBase: [3, 3, 4, 4, 5, 5, 0, 0, 0, 1, 2, 5],
    stateRegistration: null,
    stateRegistrationExempt: true,
    municipalRegistration: null,
    street: 'Av. dos Cabos',
    number: '88',
    neighborhood: 'Cavalhada',
    city: 'Porto Alegre',
    state: 'RS',
    zipBase: [9, 1, 7, 5, 0, 0, 0, 2],
    phoneBase: [5, 1, 3, 3, 2, 2, 1, 1, 2, 3, 4],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Iluminar Projetos e Instalações Ltda.',
    tradeName: 'Iluminar Projetos',
    cnpjBase: [4, 4, 5, 5, 6, 6, 0, 0, 0, 1, 2, 6],
    stateRegistration: '082.456.711.002',
    stateRegistrationExempt: false,
    municipalRegistration: '2.345.678-9',
    street: 'Rua Halfeld',
    number: '1200',
    neighborhood: 'Centro',
    city: 'Juiz de Fora',
    state: 'MG',
    zipBase: [3, 6, 0, 1, 0, 0, 0, 3],
    phoneBase: [3, 2, 3, 2, 1, 1, 1, 2, 3, 4, 5],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Centelha Comércio de Materiais Elétricos ME',
    tradeName: 'Centelha Materiais',
    cnpjBase: [5, 5, 6, 6, 7, 7, 0, 0, 0, 1, 2, 7],
    stateRegistration: null,
    stateRegistrationExempt: true,
    municipalRegistration: '8.765.432-1',
    street: 'Rua Halfeld',
    number: '450',
    neighborhood: 'Centro',
    city: 'Juiz de Fora',
    state: 'MG',
    zipBase: [3, 6, 0, 1, 0, 0, 0, 4],
    phoneBase: [3, 2, 3, 2, 1, 1, 5, 6, 7, 8, 9],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Forte Engenharia Elétrica S.A.',
    tradeName: 'Forte Engenharia',
    cnpjBase: [6, 6, 7, 7, 8, 8, 0, 0, 0, 1, 2, 8],
    stateRegistration: '130.554.881.119',
    stateRegistrationExempt: false,
    municipalRegistration: '3.210.987-6',
    street: 'Av. Agamenon Magalhães',
    number: '3450',
    neighborhood: 'Boa Viagem',
    city: 'Recife',
    state: 'PE',
    zipBase: [5, 0, 0, 7, 0, 0, 0, 5],
    phoneBase: [8, 1, 3, 3, 1, 2, 1, 2, 3, 4, 5],
    status: 'INATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Condutor Brasileiro Cabos e Fios Ltda.',
    tradeName: 'Condutor Brasileiro',
    cnpjBase: [7, 7, 8, 8, 9, 9, 0, 0, 0, 1, 2, 9],
    stateRegistration: '112.998.443.001',
    stateRegistrationExempt: false,
    municipalRegistration: '7.654.321-0',
    street: 'Rod. Anhanguera',
    number: 'Km 312',
    neighborhood: 'Distrito Industrial',
    city: 'Ribeirão Preto',
    state: 'SP',
    zipBase: [1, 4, 0, 7, 0, 0, 0, 6],
    phoneBase: [1, 6, 3, 3, 4, 4, 1, 2, 3, 4, 5],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Pulso Automação Industrial Ltda.',
    tradeName: 'Pulso Automação',
    cnpjBase: [8, 8, 9, 9, 1, 1, 0, 0, 0, 1, 3, 0],
    stateRegistration: '098.776.554.115',
    stateRegistrationExempt: false,
    municipalRegistration: '1.234.567-8',
    street: 'Av. dos Imigrantes',
    number: '1500',
    neighborhood: 'Centro',
    city: 'Caxias do Sul',
    state: 'RS',
    zipBase: [9, 5, 0, 2, 0, 0, 0, 7],
    phoneBase: [5, 4, 3, 3, 5, 5, 6, 7, 8, 9, 0],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Rede Elétrica Serviços e Manutenção ME',
    tradeName: 'Rede Elétrica',
    cnpjBase: [9, 9, 1, 1, 2, 2, 0, 0, 0, 1, 3, 1],
    stateRegistration: null,
    stateRegistrationExempt: true,
    municipalRegistration: '4.567.890-1',
    street: 'Rua Goiás',
    number: '320',
    neighborhood: 'Centro',
    city: 'Goiânia',
    state: 'GO',
    zipBase: [7, 4, 0, 1, 0, 0, 0, 8],
    phoneBase: [6, 2, 3, 2, 6, 6, 1, 2, 3, 4, 5],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Joule Materiais Elétricos Ltda.',
    tradeName: 'Joule Materiais',
    cnpjBase: [1, 2, 3, 4, 5, 6, 0, 0, 0, 1, 3, 2],
    stateRegistration: '125.667.332.110',
    stateRegistrationExempt: false,
    municipalRegistration: '9.876.543-2',
    street: 'Av. Sete de Setembro',
    number: '2200',
    neighborhood: 'Centro',
    city: 'Salvador',
    state: 'BA',
    zipBase: [4, 0, 0, 8, 0, 0, 0, 9],
    phoneBase: [7, 1, 3, 3, 7, 7, 1, 2, 3, 4, 5],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Ampère Soluções em Energia Ltda.',
    tradeName: 'Ampère Energia',
    cnpjBase: [2, 3, 4, 5, 6, 7, 0, 0, 0, 1, 3, 3],
    stateRegistration: null,
    stateRegistrationExempt: true,
    municipalRegistration: null,
    street: 'SHN Quadra 2',
    number: 'Bloco A',
    neighborhood: 'Asa Norte',
    city: 'Brasília',
    state: 'DF',
    zipBase: [7, 0, 7, 0, 2, 0, 0, 1],
    phoneBase: [6, 1, 3, 3, 8, 8, 1, 2, 3, 4, 5],
    status: 'ATIVO',
    createdBy: SEED_AUTHOR,
  },
  {
    legalName: 'Delta Força Comercial Elétrica S.A.',
    tradeName: 'Delta Força',
    cnpjBase: [3, 4, 5, 6, 7, 8, 0, 0, 0, 1, 3, 4],
    stateRegistration: '142.998.110.004',
    stateRegistrationExempt: false,
    municipalRegistration: '5.432.109-8',
    street: 'Av. Dom Luís',
    number: '1300',
    neighborhood: 'Aldeota',
    city: 'Fortaleza',
    state: 'CE',
    zipBase: [6, 0, 1, 1, 0, 0, 0, 2],
    phoneBase: [8, 5, 3, 3, 9, 9, 1, 2, 3, 4, 5],
    status: 'INATIVO',
    createdBy: SEED_AUTHOR,
  },
]

/** Monta um CompanyResponse a partir da semente simplificada. */
function build(seed: CompanySeed, index: number): CompanyResponse {
  return {
    uuid: `00000000-0000-4000-8000-${String(index + 1).padStart(12, '0')}`,
    legalName: seed.legalName,
    tradeName: seed.tradeName,
    code: `EMP${String(index + 1).padStart(6, '0')}`,
    cnpj: formatCnpj(makeCnpj(seed.cnpjBase)),
    stateRegistration: seed.stateRegistration,
    stateRegistrationExempt: seed.stateRegistrationExempt,
    municipalRegistration: seed.municipalRegistration,
    address: {
      street: seed.street,
      number: seed.number,
      neighborhood: seed.neighborhood,
      city: seed.city,
      state: seed.state,
      zipCode: formatZip(seed.zipBase.join('')),
    },
    status: seed.status,
    createdAt: SEED_TIMESTAMP,
    updatedAt: SEED_TIMESTAMP,
    createdBy: seed.createdBy,
    updatedBy: null,
  }
}

/** Lista de empresas fictícias pronta para uso em testes/dev. */
export const mockCompanies: ReadonlyArray<CompanyResponse> = RAW.map(build)

// Silencia o "unused" de formatPhone — útil se algum seed precisar telefones.
void formatPhone