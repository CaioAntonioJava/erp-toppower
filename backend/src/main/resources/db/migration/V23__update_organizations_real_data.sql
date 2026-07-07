-- =============================================================================
-- V23__update_organizations_real_data.sql
--
-- Atualiza as duas Organizations padrão (criadas pelo BootstrapRunner com
-- CNPJs placeholders) para os dados reais da Top Power.
--
-- Identificação: pelo CNPJ placeholder atual —
--   11.222.333/0001-81 -> Top Power Engenharia Ltda ME (CNPJ 13.433.616/0001-06)
--   11.444.777/0001-61 -> Top Power Materiais Ltda ME (CNPJ 59.530.698/0001-08)
--
-- Idempotente: o UPDATE ... WHERE cnpj = <placeholder> só afeta linhas cujo
-- CNPJ ainda é o placeholder. Em bases já atualizadas (ou criadas depois do
-- BootstrapRunner corrigido), nenhum registro casa e a migration é no-op.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Empresa 1: Top Power Engenharia Ltda ME
-- -----------------------------------------------------------------------------
UPDATE organizations
SET corporate_name        = 'TOP POWER ENGENHARIA LTDA ME',
    trade_name            = 'TOP POWER ENGENHARIA',
    cnpj                  = '13.433.616/0001-06',
    state_registration    = '671.137.811.110',
    municipal_registration = '29764.01-6',
    zip_code              = '13170-700',
    street                = 'AVENIDA REBOUCAS',
    number                = '4465',
    complement            = NULL,
    district              = 'RES. VECCON',
    city                  = 'SUMARE',
    state                 = 'SP'
WHERE cnpj = '11.222.333/0001-81';

-- -----------------------------------------------------------------------------
-- Empresa 2: Top Power Materiais Ltda ME
-- -----------------------------------------------------------------------------
UPDATE organizations
SET corporate_name        = 'TOP POWER MATERIAIS LTDA ME',
    trade_name            = 'TOP POWER MATERIAIS',
    cnpj                  = '59.530.698/0001-08',
    state_registration    = '671.700.534.116',
    municipal_registration = '62965010',
    zip_code              = '13171-456',
    street                = 'RUA JOAO RAVAGNANI',
    number                = '36',
    complement            = NULL,
    district              = 'JARDIM RESIDENCIAL RAVAGNANI',
    city                  = 'SUMARE',
    state                 = 'SP'
WHERE cnpj = '11.444.777/0001-61';