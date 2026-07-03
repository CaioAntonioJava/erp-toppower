/**
 * Barrel dos mocks de API. Apenas para desenvolvimento/teste manual.
 *
 * Importado pelos arquivos de `src/api/*.api.ts` quando
 * `VITE_USE_MOCKS === 'true'`. Não use este barrel diretamente em código
 * de produção.
 */

export * as companyApiMock from './company.api.mock'
export * as customerApiMock from './customer.api.mock'
export * as sellerApiMock from './seller.api.mock'
export * as productApiMock from './product.api.mock'