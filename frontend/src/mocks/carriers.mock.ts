/**
 * Mocks de transportadoras (Carrier).
 *
 * Apenas para desenvolvimento/teste manual. NÃO usar em produção.
 *
 * Total: 6 transportadoras — mistura de ATIVAS e INATIVAS, cobrindo todos
 * os valores do enum CarrierName. UUIDs seguem o mesmo padrão determinístico
 * das demais seeds (`00000000-0000-4000-8000-NNNNNNNNNNNN`), começando em
 * `...2001` para não colidir com sellers (1-12), products, etc.
 */

import type {
  CarrierName,
  CarrierResponse,
  CarrierStatus,
} from '../types/carrier'
import { SEED_AUTHOR, SEED_TIMESTAMP } from './helpers'

interface CarrierSeed {
  carrierName: CarrierName | null
  freightValue: number | null
  status: CarrierStatus
}

const SEEDS: CarrierSeed[] = [
  { carrierName: 'CORREIOS_SEDEX', freightValue: 45.9, status: 'ATIVO' },
  { carrierName: 'CORREIOS_PAC', freightValue: 32.5, status: 'ATIVO' },
  { carrierName: 'JADLOG', freightValue: 58.0, status: 'ATIVO' },
  { carrierName: 'OUTRAS_TRANSPORTADORAS', freightValue: 70.0, status: 'ATIVO' },
  { carrierName: 'CORREIOS_SEDEX', freightValue: 50.0, status: 'INATIVO' },
  { carrierName: null, freightValue: 40.0, status: 'ATIVO' },
]

function build(seed: CarrierSeed, index: number): CarrierResponse {
  return {
    uuid: `00000000-0000-4000-8000-${String(2001 + index).padStart(12, '0')}`,
    carrierName: seed.carrierName,
    freightValue: seed.freightValue,
    status: seed.status,
    createdAt: SEED_TIMESTAMP,
    updatedAt: SEED_TIMESTAMP,
    createdBy: SEED_AUTHOR,
    updatedBy: null,
  }
}

export const mockCarriers: ReadonlyArray<CarrierResponse> = SEEDS.map(build)