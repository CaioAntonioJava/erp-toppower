# AGENTS.md

Guia de referência para agentes de IA (e desenvolvedores) que atuam neste repositório.
O projeto é um **ERP full-stack** para a empresa brasileira TopPower (engenharia/elétrica),
composto por um backend Spring Boot e um frontend React, orquestrados via Docker Compose.

> **Idioma:** todo código, comentários, mensagens de erro e textos de UI devem ser
> escritos em **Português do Brasil (`pt-BR`)**, salvo quando um identificador/termo
> técnico exigir outro idioma. Mantenha esse padrão ao contribuir.

> **Commits e push:** mensagens de commit devem ser escritas em **inglês**,
> seguindo o padrão [Conventional Commits](https://www.conventionalcommits.org/)
> (`feat:`, `fix:`, `refactor:`, `chore:`, etc.). Commits devem ser atômicos e
> separados por escopo sempre que fizer sentido (ex.: um commit para backend e
> outro para frontend, ou um para a feature e outro para correção de bug).
> Push para `main` apenas após verificar que o build e lint passam.

---

## Visão geral da stack

| Camada        | Tecnologia                                                       |
|---------------|------------------------------------------------------------------|
| Backend       | Java 17, Spring Boot 4.1, Spring Data JPA/Hibernate, Maven       |
| Banco de dados| MySQL 8 (utf8mb4)                                                |
| Autenticação  | Spring Security + JWT (JJWT 0.12.6, HS256, BCrypt)               |
| PDF           | Thymeleaf + OpenHTMLtoPDF                                        |
| API docs      | SpringDoc OpenAPI (Swagger UI)                                   |
| Frontend      | React 19 + Vite 8 + TypeScript, Tailwind CSS 4, React Router 7   |
| HTTP client   | axios                                                            |
| Linter FE     | oxlint                                                           |
| Containers    | Docker (multi-stage) + Docker Compose                            |

Estrutura de diretórios (não é um workspace formal; backend e frontend são projetos
independentes unificados apenas pelo `docker-compose.yml`):

```
erp-toppower/
├── docker-compose.yml     # db + backend + frontend (rede erp-net)
├── .env.example           # DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION_HOURS
├── README.md              # Guia de setup com Docker (pt-BR)
├── backend/               # Spring Boot 4.1 / Java 17 / Maven
└── frontend/              # React 19 + Vite + TypeScript + Tailwind
```

---

## Comandos essenciais

### Docker (pilha completa, a partir da raiz)
```bash
cp .env.example .env          # edite DB_PASSWORD e JWT_SECRET
docker compose up -d --build  # sobe db + backend + frontend
docker compose logs -f backend
docker compose down           # para, mantendo os volumes
docker compose down -v        # para e apaga os volumes (MySQL + uploads)
```

### Backend (a partir de `backend/`)
```bash
./mvnw clean package                            # build do JAR (Windows: mvnw.cmd)
./mvnw spring-boot:run                           # roda localmente (requer MySQL + .env)
./mvnw test                                      # todos os testes
./mvnw test -Dtest=SalesPdfServiceTest           # classe única
```
- Porta: **8081**
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

### Frontend (a partir de `frontend/`)
```bash
npm ci          # instala dependências
npm run dev     # dev server (proxy /api -> http://localhost:8081)
npm run build   # tsc -b && vite build -> dist/
npm run lint    # oxlint
npm run preview # serve o build
```
- Porta dev: **5173** (Vite); produção (nginx): **8080**
- O dev server faz proxy de `/api` para o backend em `:8081`, evitando CORS.

### URLs e credenciais de referência
- Frontend (Docker): `http://localhost:8080`
- Backend: `http://localhost:8081`
- MySQL: `localhost:3306`, banco `erp-toppower-api`
- Admin padrão (bootstrap idempotente): `admin@toppower.com.br` / `Admin@123`
  (override via `APP_BOOTSTRAP_ADMIN_EMAIL` / `APP_BOOTSTRAP_ADMIN_PASSWORD`).

---

## Backend — arquitetura e convenções

### Estrutura por funcionalidade (package-by-feature)
Cada módulo de negócio é um pacote em
`backend/src/main/java/br/com/toppower/erp_toppower/<modulo>/` com layout fixo:

```
<modulo>/
├── controller/   # @RestController — endpoints REST
├── service/      # @Service — lógica de negócio, @Transactional
├── entity/       # JPA @Entity
├── repository/   # Spring Data JpaRepository
├── dto/          # records: <Entity>CreateRequest / UpdateRequest / Response
├── mapper/       # classe final com métodos estáticos (toEntity/toResponse/applyUpdate)
├── enums/        # enums do módulo (status, tipos)
└── exception/    # exceções de negócio específicas
```

Módulos existentes: `auth`, `carrier`, `cep`, `company`, `contract`, `customer`,
`organization`, `person`, `product`, `profile`, `sales` (`quotation`, `salesorder`,
`technicalproposal`, `pdf`), `seller`, `stock`, `supplier`, `user`, `userorganization`.
Pacotes transversais: `common`, `config`, `security`.

### Entidades e persistência
- **PKs são UUID** (`BINARY(16)`), sem FKs físicas; referências via UUID.
- `BaseEntity` provê `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
  (auditoria via `@EnableJpaAuditing` + `AuditorAwareImpl`).
- Entidades de negócio estendem `OrganizationScopedEntity`, que adiciona a coluna
  `organization_uuid` e um `@Filter` Hibernate (`organizationFilter`).
- `spring.jpa.hibernate.ddl-auto=update` — o Hibernate atualiza o schema; scripts SQL
  adicionais rodam depois via `spring.sql.init`.

### Multi-tenancy (organização)
- O filtro `OrganizationFilterAspect` (AOP `@Before` em repositories) habilita
  `organizationFilter` na Session Hibernate usando o `OrganizationContext` (ThreadLocal),
  aplicando automaticamente `WHERE organization_uuid = :organizationUuid` em consultas
  JPQL/Criteria de entidades escopadas. **Não filtra queries nativas.**
- O `OrganizationContextFilter` (após o filtro JWT) lê o header `X-Organization-Id`,
  valida existência/ativo/vínculo do usuário e popula o `OrganizationContext`.
- `ROLE_ADMIN` tem acesso a todas as organizações; demais usuários precisam de
  vínculo em `UserOrganization`.

### Autenticação
- JWT stateless (HS256). `SecurityConfig`: sessões stateless, CSRF desativado,
  `BCryptPasswordEncoder`, `DaoAuthenticationProvider` com
  `hideUserNotFoundExceptions=true` (evita enumeração de usuários).
- `@EnableMethodSecurity` + `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` nos
  controllers.
- `JwtAuthenticationFilter` extrai o Bearer token e popula o `SecurityContext`.
- Rotas públicas: `/api/v1/auth/login`, `/logos/**` (para PDFs), Swagger.
- `JwtService` valida que a chave tem ≥ 32 bytes na inicialização; o token carrega
  `subject=email` + claim `role`; expiração via `jwt.expiration-hours` (padrão 24h).
- `BootstrapRunner` (`CommandLineRunner @Order(0)`) semeia duas organizações padrão
  e um admin default de forma idempotente (toggle via `app.bootstrap.enabled`).

### Migrations de banco
- Scripts em `backend/src/main/resources/db/migration/` com nome `V<n>__<desc>.sql`
  (convenção estilo Flyway, **mas sem Flyway/Liquibase**).
- Executados pelo `spring.sql.init` (`mode=always`) a cada boot; a lista exata de
  scripts está enumerada em `spring.sql.init.schema-locations` no
  `application.properties`. **Ao adicionar um novo script, também o inclua nessa lista**
  — mantenha apenas scripts idempotentes.
- `spring.jpa.defer-datasource-initialization=true` garante que o SQL init rode após
  o `ddl-auto=update`.

### CEP (códigos postais brasileiros)
- `backend/src/main/resources/cep/ceps_brasil.csv` (~850k registros) embarcado no JAR.
- Importado automaticamente no boot se a tabela `ceps` estiver vazia
  (`app.cep.import.auto=true`, batch `INSERT IGNORE` de `app.cep.import.batch-size`).
- Configurável via `CEP_CSV_PATH`, `CEP_BATCH_SIZE`, `CEP_AUTO_IMPORT`.

### PDF
- `sales/pdf/` com `SalesPdfService` (Thymeleaf → OpenHTMLtoPDF), `ImageEmbedder`
  (logos como data URIs), `PdfModelBuilder`, etc.
- Templates em `backend/src/main/resources/templates/pdf/`
  (`quotation.html`, `technical-proposal.html`, `sales-order.html`, `contract.html`,
  `styles.html`, `fragments/issuer-header.html`).

### Tratamento de erros
- `GlobalExceptionHandler` (`@RestControllerAdvice`) mapeia exceções de domínio para
  HTTP (404 NotFound, 409 Duplicate, 422 `InsufficientStockException`, 400 validação)
  e retorna um `ApiError` (`status`, `message`, `timestamp`, `fieldErrors`).
- Exceções específicas por módulo, na pasta `exception/` de cada um.

### Testes (backend)
- JUnit 5 em `backend/src/test/java/...`. Apenas testes unitários (utilitários,
  listeners, validadores, smoke test de PDF). Sem testes de integração com DB.
- Rode com `./mvnw test`.

### Convenções de código (backend)
- **Construtor injection** (sem `@Autowired` em campos); controllers recentes usam
  Lombok `@RequiredArgsConstructor`.
- DTOs são **`record`** com sufixos `CreateRequest` / `UpdateRequest` / `Response`.
- Mappers: classe `final` com métodos estáticos — sem MapStruct.
- Controllers: base path `/api/v1/<recurso>`, anotações Swagger completas
  (`@Operation`, `@ApiResponses`, `@Tag`, `@SecurityRequirement(bearerAuth)`).
- **Soft delete**: `DELETE` apenas marca status como `INATIVO` (não remove fisicamente).
- Enums de status tipicamente `ATIVO` / `INATIVO`.
- `@Transactional` nos services; `readOnly = true` em consultas.
- `@UpperCase` + `UpperCaseFieldListener` auto-maiusculizam campos `String`
  (convenção brasileira para nomes/códigos).
- Locale `pt_BR`, timezone `America/Sao_Paulo`.

---

## Frontend — arquitetura e convenções

### Estrutura
```
frontend/src/
├── main.tsx            # BrowserRouter > ThemeProvider > AuthProvider > OrganizationProvider
├── App.tsx             # Rotas (públicas, protegidas, admin, print)
├── index.css           # Tailwind + tokens de tema + dark mode + estilos de print
├── api/                # Um arquivo por entidade (axios) + client.ts
├── components/
│   ├── layout/         # AppLayout, Sidebar, Topbar, Footer, AuthCard
│   ├── ui/             # Primitivos: Button, Input, Select, Spinner, Alert, Badge, ...
│   ├── client/  contract/  sales/   # Componentes específicos de domínio
│   ├── ProtectedRoute.tsx
│   └── AdminRoute.tsx
├── context/            # AuthContext, OrganizationContext, ThemeContext
├── hooks/              # useEntityList, useActiveCarriers, useFieldTouched
├── lib/                # brazilianStates, documents, errors, money
├── pages/              # Um arquivo por rota (~36: List/Form/Detail/Print)
└── types/              # Tipos por entidade + api.ts (PagedResponse, etc.)
```

### Estado
- Sem biblioteca externa (Redux/Zustand). Estado global via **React Context**:
  - `AuthContext`: user, isAuthenticated, hasProfile, signIn/signOut/refresh.
    Persiste o JWT em `localStorage` (`erp_toppower_token`).
  - `OrganizationContext`: organização ativa + lista, `revision` para forçar remount.
    Persiste o UUID ativo em `localStorage` (`erp_toppower_org`).
  - `ThemeContext`: light/dark (toggle da classe `.dark` no `<html>`).
- `useEntityList<T>` centraliza lógica de listas: paginação, busca com debounce,
  seleção em massa, ativar/inativar com modais de confirmação.

### API client
- `src/api/client.ts`: instância axios única, `baseURL = import.meta.env.VITE_API_URL ?? ''`.
- Interceptor de requisição injeta `Authorization: Bearer <token>` e `X-Organization-Id`.
- Interceptor de resposta: em 401 limpa o token e redireciona para `/login`.
- `src/api/<entity>.api.ts`: funções nomeadas (`listX`, `searchX`, `getX`, `createX`,
  `updateX`, `inactivateX`), base path `BASE = '/api/v1/<recurso>'`.

### Rotas (`App.tsx`)
- Públicas: `/login`, `/select-organization`.
- Protegidas (dentro de `ProtectedRoute` + `AppLayout`): dashboard, perfil e CRUDs de
  companies, customers, suppliers, sellers, products, quotations, sales-orders,
  technical-proposals, contracts.
- Admin (`AdminRoute`): `/users`, `/carriers`, `/organizations`.
- Print (protegidas, sem `AppLayout`): `/<entity>/:id/pdf`.

### UI
- Tailwind CSS 4 (config CSS-first via `@import 'tailwindcss'` + `@theme` em `index.css`).
  Cor primária `#0271e3`, foco `#ffae00`. Dark mode via variante `dark:`.
- Ícones: `lucide-react`. Sem biblioteca de componentes — primitivos em `components/ui/`.

### Build / lint
- Build: `tsc -b && vite build`. `tsconfig.app.json` com `verbatimModuleSyntax`,
  `noUnusedLocals`, `noUnusedParameters`, `noFallthroughCasesInSwitch`.
- Lint: **oxlint** (`npm run lint`). Sem ESLint/Prettier. `.oxlintrc.json` habilita
  plugins `react`/`typescript`/`oxc` com `react/rules-of-hooks: error`.
- **Sem framework de testes** no frontend (nenhum vitest/jest configurado).

### Convenções de código (frontend)
- Uma página por arquivo em `src/pages/`, nome `<Entity><List|Form|Detail|Print>Page.tsx`,
  export default nomeado (`export function ProductsListPage()`).
- Tipos em `src/types/<entity>.ts` espelham os DTOs do backend; tipos compartilhados
  em `src/types/api.ts`.
- Use `import type` para imports somente de tipo (compatível com `verbatimModuleSyntax`).
- Classes Tailwind diretas; preferir tokens `bg-primary`/`text-primary`.
- Hooks `useAuth()` / `useOrganization()` lançam erro se usados fora do provider.

---

## Docker / deploy

`docker-compose.yml` define três serviços na rede `erp-net`:

| Serviço | Imagem base            | Porta host | Observações                                   |
|---------|------------------------|------------|-----------------------------------------------|
| db      | mysql:8.0              | 3306       | Volume `mysql_data`, utf8mb4                  |
| backend | eclipse-temurin:17-jre | 8081       | Build multi-stage Maven; uploads em volume    |
| frontend| nginx:alpine           | 8080       | Build Node 20; `VITE_API_URL=/api`; proxy nginx |

- `backend/Dockerfile`: `maven:3.9-eclipse-temurin-17` (build) → `eclipse-temurin:17-jre`
  (runtime), `-DskipTests`, expõe 8081.
- `frontend/Dockerfile`: `node:20-alpine` (build) → `nginx:alpine` servindo `dist/`.
- `frontend/nginx.conf`: serve o SPA, faz proxy de `/api/` → `backend:8081`, fallback
  SPA, limite de upload 2MB.

---

## Notas importantes para agentes

- **Idioma**: mantenha comentários, mensagens, logs e UI em `pt-BR`.
- **Ao adicionar um módulo de backend**: replique o layout
  `controller/service/entity/repository/dto/mapper/enums/exception`; entidades de
  negócio devem estender `OrganizationScopedEntity`; crie DTOs como `record` e mapper
  com métodos estáticos.
- **Ao adicionar uma migration SQL**: crie `V<n>__<desc>.sql` em
  `backend/src/main/resources/db/migration/` **e** adicione o caminho em
  `spring.sql.init.schema-locations` no `application.properties`. Use apenas SQL
  idempotente (`CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT
  EXISTS`, etc.).
- **Soft delete**: nunca remova registros fisicamente; troque status para `INATIVO`.
- **Multi-tenancy**: o `organizationFilter` é automático em queries JPQL/Criteria de
  entidades escopadas — **não filtra queries nativas**; trate o escopo manualmente
  quando usar SQL nativo.
- **Frontend sem testes**: se testes forem necessários, o vitest precisa ser
  configurado do zero.
- **Não commitear segredos**: `.env` e `backend/.env` estão no `.gitignore`; use os
  `.env.example` como template.
- Antes de um commit, rode lint/testes aplicáveis: `npm run lint` (frontend) e
  `./mvnw test` (backend).