-- =============================================================================
-- V40__seed_organizations_top_power.sql
--
-- Seed inicial: garante que as duas empresas Top Power existam na tabela
-- `organizations` ao iniciar o projeto, com todos os dados cadastrais reais
-- (razão social, CNPJ, IE, IM, endereço, prefixos de proposta e contrato).
--
-- Empresas:
--   1) TOP POWER ENGENHARIA LTDA ME  — CNPJ 13.433.616/0001-06 — prefixos PT/CT
--   2) TOP POWER MATERIAIS  LTDA ME  — CNPJ 59.530.698/0001-08 — prefixos PL/CL
--
-- Idempotência:
--   O INSERT ... SELECT ... WHERE NOT EXISTS (guardado por CNPJ na tabela
--   organizations) só cria a linha quando a empresa ainda não existe. Em
--   boots subsequentes a subquery retorna 0 linhas e nada é inserido, então
--   o script pode rodar a cada boot (spring.sql.init.mode=always) sem erro.
--   Não utiliza ON DUPLICATE KEY UPDATE para não sobrescrever edições feitas
--   pelo usuário via tela de cadastro de Organization.
--
-- Colunas:
--   - id gerado automaticamente pelo AUTO_INCREMENT (não é especificado no INSERT).
--   - Os valores de created_at/updated_at usam CURRENT_TIMESTAMP (default da tabela).
--     As colunas logo_url e contract_default_description ficam NULL no seed.
--
-- Dependências:
--   - Tabela `organizations` criada por V18 (CREATE TABLE IF NOT EXISTS).
--   - Colunas `proposal_prefix` e `contract_prefix` adicionadas por V25/V29
--     (ou pelo Hibernate ddl-auto=update). Ambas são NOT NULL, por isso o
--     seed sempre fornece valor.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Empresa 1: TOP POWER ENGENHARIA LTDA ME
-- -----------------------------------------------------------------------------
INSERT INTO organizations (
    corporate_name,
    trade_name,
    cnpj,
    state_registration,
    municipal_registration,
    zip_code,
    street,
    number,
    district,
    city,
    state,
    status,
    proposal_prefix,
    contract_prefix,
    created_at,
    updated_at
)
SELECT 'TOP POWER ENGENHARIA LTDA ME',
       'TOP POWER ENGENHARIA',
       '13.433.616/0001-06',
       '671.137.811.110',
       '29764.01-6',
       '13170-700',
       'AVENIDA REBOUCAS',
       '4465',
       'RES. VECCON',
       'SUMARE',
       'SP',
       'ATIVO',
       'PT',
       'CT',
       now(),
       now()
WHERE NOT EXISTS (
    SELECT 1 FROM organizations WHERE cnpj = '13.433.616/0001-06'
);

-- -----------------------------------------------------------------------------
-- Empresa 2: TOP POWER MATERIAIS LTDA ME
-- -----------------------------------------------------------------------------
INSERT INTO organizations (
    corporate_name,
    trade_name,
    cnpj,
    state_registration,
    municipal_registration,
    zip_code,
    street,
    number,
    district,
    city,
    state,
    status,
    proposal_prefix,
    contract_prefix,
    created_at,
    updated_at
)
SELECT 'TOP POWER MATERIAIS LTDA ME',
       'TOP POWER MATERIAIS',
       '59.530.698/0001-08',
       '671.700.534.116',
       '62965010',
       '13171-456',
       'RUA JOAO RAVAGNANI',
       '36',
       'JARDIM RESIDENCIAL RAVAGNANI',
       'SUMARE',
       'SP',
       'ATIVO',
       'PL',
       'CL',
       now(),
       now()
WHERE NOT EXISTS (
    SELECT 1 FROM organizations WHERE cnpj = '59.530.698/0001-08'
);