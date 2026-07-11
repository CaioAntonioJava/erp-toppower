-- ============================================================================
-- Migration V31: Adiciona contract_default_description na tabela organizations
-- ============================================================================
-- Permite que cada empresa (Organization) tenha um texto padrão em HTML
-- que será pré-preenchido na descrição de novos contratos.
--
-- Idempotência:
--   Padrão PREPARE/EXECUTE dinâmico idêntico ao de V25/V29, pois
--   `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` não é suportado no MySQL
--   utilizado neste projeto. Necessário porque o Hibernate
--   ddl-auto=update pode ter criado a coluna em boots anteriores.
-- ============================================================================

-- 1. Adiciona a coluna (TEXT permite HTML com formatação, ex.: <strong>)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'organizations'
      AND COLUMN_NAME = 'contract_default_description');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE organizations ADD COLUMN contract_default_description TEXT NULL COMMENT ''Texto HTML padrão pré-preenchido na descrição de novos contratos.''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Backfill: insere o texto padrão do contrato para a Top Power Materiais
--    (CNPJ 59.530.698/0001-08). Idempotente: só atualiza se a coluna estiver
--    nula ou vazia (evita sobrescrever edições do usuário em boots
--    posteriores).
UPDATE organizations
SET contract_default_description = '<p>Contrato que entre si fazem, <strong>TOP POWER MATERIAIS LTDA</strong>, pessoa jur&iacute;dica de direito privado, situada na R Jo&atilde;o Ravagnani, n&deg;36, JARDIM RESIDENCIAL RAVAGNANI no munic&iacute;pio de Sumar&eacute;, CEP: 13170-700, Estado de S&atilde;o Paulo, inscrita no CNPJ/MF sob o n&deg; 59.530.698/0001-08, com Inscri&ccedil;&atilde;o Municipal n&deg; 62965010, neste ato representada pelo Sr. <strong>Fernando Willian Toscano</strong> portador da c&eacute;dula de identidade RG n&deg; 35.219.008-5-SSP/SP e do CPF: 057.140.056-60 &mdash; <strong>CONTRATANTE</strong>.</p>'
WHERE cnpj = '59.530.698/0001-08'
  AND (contract_default_description IS NULL OR contract_default_description = '');
