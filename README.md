# ERP TopPower

Sistema ERP da TopPower — backend Spring Boot (Java 17) + frontend React/Vite + MySQL 8.

## Stack

| Camada | Tecnologia |
|--------|------------|
| Backend | Spring Boot 4.1 / Java 17 / Maven |
| Frontend | React 19 + Vite + TypeScript + Tailwind |
| Banco | MySQL 8.0 |
| PDFs | Thymeleaf + OpenHTMLtoPDF |
| Auth | JWT (JJWT 0.12) |

## Rodando com Docker

Pré-requisito: **Docker Desktop** instalado e em execução.

### 1. Clonar e configurar

```bash
git clone https://github.com/CaioAntonioJava/erp-toppower.git
cd erp-toppower

# Criar o arquivo de variáveis a partir do template
cp .env.example .env
```

Edite o `.env` e preencha:

```env
DB_PASSWORD=<uma-senha-forte>
JWT_SECRET=<secret-gerado-com-openssl-rand-base64-48>
```

> ⚠️ O `.env` está no `.gitignore` e **não** deve ser commitado (contém segredos).

### 2. Subir o projeto

```bash
docker compose up -d --build
```

Na primeira vez o Docker baixa as imagens base, compila o backend (Maven) e o frontend (Vite) dentro dos containers, e sobe os 3 serviços. O MySQL sobe primeiro; o backend aguarda o banco ficar saudável antes de iniciar.

> O primeiro boot do backend demora ~1-2 min a mais porque ele auto-importa a base de CEPs (~850k registros) embarcada no JAR.

### 3. Acessar

| Serviço | URL |
|---------|-----|
| Aplicação (frontend) | http://localhost:8080 |
| Swagger UI (API) | http://localhost:8081/swagger-ui.html |
| OpenAPI JSON | http://localhost:8081/v3/api-docs |

## Comandos úteis

```bash
# Logs em tempo real
docker compose logs -f
docker compose logs -f backend      # só o backend

# Parar (mantém os dados)
docker compose down

# Parar e apagar os dados (banco + uploads)
docker compose down -v

# Reconstruir após mudar código
docker compose up -d --build

# Reiniciar um serviço
docker compose restart backend
```

## Portas

| Porta | Serviço |
|-------|---------|
| 8080 | Frontend (nginx) |
| 8081 | Backend (Spring Boot) |
| 3306 | MySQL |

Se a porta 3306 já estiver em uso na sua máquina (MySQL local), altere o mapeamento no `docker-compose.yml` para `"3307:3306"`.

## Persistência

- `mysql_data` — banco de dados (volume nomeado)
- `backend_uploads` — logos das Organizations em `/app/uploads` (volume nomeado)

Ambos sobrevivem a `docker compose down`. Só são apagados com `docker compose down -v`.

## Variáveis de ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `DB_NAME` | `erp-toppower-api` | Nome do banco |
| `DB_PASSWORD` | — | Senha do root do MySQL |
| `JWT_SECRET` | — | Secret para assinar tokens JWT |
| `JWT_EXPIRATION_HOURS` | `24` | Validade do token JWT |
| `CEP_AUTO_IMPORT` | `true` | Auto-importa a base de CEPs no boot se a tabela estiver vazia |

## Estrutura dos arquivos Docker

```
erp-toppower/
├── docker-compose.yml      # orquestra db + backend + frontend
├── .env.example            # template de variáveis (copiar para .env)
├── backend/
│   ├── Dockerfile          # build Maven (multi-stage) -> JRE 17
│   └── .dockerignore
└── frontend/
    ├── Dockerfile          # build Vite (multi-stage) -> nginx
    ├── nginx.conf          # proxy /api + SPA fallback
    └── .dockerignore
```