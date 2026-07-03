# Mocks de teste (frontend)

Dados fictícios para **desenvolvimento e teste manual** do frontend.
**NÃO** devem ser importados em código de produção (páginas, hooks,
camada de api fora do dispatch).

## Como ativar

Defina `VITE_USE_MOCKS=true` em `frontend/.env.development.local`
(já está nesse estado por padrão — basta rodar `npm run dev`).

```ini
# frontend/.env.development.local
VITE_USE_MOCKS=true
```

Para voltar a bater no backend real, troque para `false` (ou remova o
arquivo). O arquivo termina em `.local`, então está no `.gitignore` e
não é commitado.

Quando o flag está ativo, **todas as funções** de `src/api/company.api.ts`,
`customer.api.ts`, `seller.api.ts` e `product.api.ts` retornam os mocks
em vez de fazer HTTP. As páginas (`CompaniesListPage`,
`CustomersListPage`, etc.) continuam importando dos mesmos caminhos —
nenhuma mudança nas páginas ou nos hooks é necessária.

## Conteúdo

| Arquivo                                    | Export           | Quantidade | Observação                                |
| ------------------------------------------ | ---------------- | ---------- | ---------------------------------------- |
| `companies.mock.ts`                        | `mockCompanies`  | 12         | PJ com CNPJ válido (mod-11)              |
| `customers.mock.ts`                        | `mockCustomers`  | 12         | PF com CPF válido (mod-11)               |
| `sellers.mock.ts`                          | `mockSellers`    | 12         | PF + comissão (1 vendedor sem comissão)  |
| `products.mock.ts`                         | `mockProducts`   | 12         | 3 `UnitType` (UNIDADE/METROS/BOBINA)     |
| `helpers.ts`                               | (interno)        | —          | Geração de CPF/CNPJ, formatação, fixos   |
| `index.ts`                                 | barrel + `asPaged()` | —      | Re-exporta seeds                          |
| `api/store.ts`                             | (interno)        | —          | Estado em memória mutável                |
| `api/_helpers.ts`                          | (interno)        | —          | `delay`, `mockError`, `pagedSlice`       |
| `api/company.api.mock.ts`                  | (interno)        | —          | Implementação mockada de `company.api.ts`|
| `api/customer.api.mock.ts`                 | (interno)        | —          | Implementação mockada de `customer.api.ts`|
| `api/seller.api.mock.ts`                   | (interno)        | —          | Implementação mockada de `seller.api.ts` |
| `api/product.api.mock.ts`                  | (interno)        | —          | Implementação mockada de `product.api.ts`|

## Como usar direto (em testes)

```ts
import { mockCompanies, mockCustomers, asPaged } from '@/mocks'

// Injetar direto num teste de componente:
const companies = mockCompanies

// Embrulhar como resposta paginada (mesma shape do backend):
const paged = asPaged(mockProducts, 0, 20)
// -> { content, page, size, totalElements, totalPages, first, last }
```

## Garantias

- **CPF/CNPJ válidos** — gerados pelo mesmo algoritmo mod-11 usado em
  `lib/documents.ts` e em `DocumentValidator.java` no backend. Passam
  direto pelos formulários.
- **Tipos idênticos ao backend** — `*Response` espelha `*Response` DTO.
  Não há "forma simplificada".
- **Determinísticos** — `createdAt` inicial fixado em
  `2025-06-01T12:00:00Z`, `createdBy` fixado em `seed@toppower.local`.
  Bom para snapshots de teste.
- **Cobertura de status** — todos os mocks têm itens `ATIVO` e `INATIVO`,
  e empresas cobrem tanto o caso com IE quanto o caso **isento**
  (`stateRegistrationExempt = true`).
- **Latência simulada (~180ms)** — para que os estados de `loading` das
  páginas apareçam durante o dev.
- **Erros no formato do axios** — `mockError(status, message)` produz um
  `Error` com `.response.data` no mesmo formato que `toApiError(err)`
  espera, então a UI exibe a mensagem correta sem tratamento especial.
- **Mutações refletem na listagem** — `create*`, `update*`, `inactivate*`
  e `activate*` alteram o store em memória. As listas recarregam
  automaticamente porque `useEntityList` faz `load()` após cada operação.
  As alterações se perdem no reload da página (não há persistência em
  `localStorage`).

## O que NÃO está incluso

- Senhas / hash — usuários não têm mocks aqui (use `/api/v1/auth/login`
  ou ajuste o `AuthContext` no modo mock).
- Vínculos entre entidades (ex.: `Quotation` com `customerId` real) —
  esses ficam para fixtures mais elaboradas conforme a necessidade.

## Reset dos mocks

Como o store vive em memória, basta recarregar a página (F5) para voltar
ao estado inicial dos seeds. Não há efeito colateral entre reloads.