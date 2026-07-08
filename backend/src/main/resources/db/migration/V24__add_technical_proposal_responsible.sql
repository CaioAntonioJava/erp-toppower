-- =============================================================================
-- V24__add_technical_proposal_responsible.sql
--
-- Adiciona os campos opcionais `technical_responsible` (nome do responsável
-- técnico) e `email` (e-mail de contato) na tabela `technical_proposals`.
--
-- Contexto:
--   O formulário de Proposta Técnica passou a permitir informar o nome do
--   responsável técnico e seu e-mail de contato. Ambos os campos são
--   opcionais (NULL permitido) e livres (sem validação/formatação no
--   backend — o e-mail é apenas um campo de texto digitado pelo usuário).
--
-- Posicionamento: após `description`, mantendo a ordem lógica do cabeçalho
-- da proposta (descrição → responsável → e-mail → status).
--
-- Idempotência:
--   Padrão PREPARE/EXECUTE dinâmico idêntico ao da V18/V19, pois
--   `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` não é suportado no MySQL
--   utilizado neste projeto.
-- =============================================================================

-- technical_responsible (nome do responsável técnico)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'technical_proposals'
      AND COLUMN_NAME = 'technical_responsible');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE technical_proposals ADD COLUMN technical_responsible VARCHAR(150) NULL AFTER description',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- email (e-mail de contato)
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'technical_proposals'
      AND COLUMN_NAME = 'email');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE technical_proposals ADD COLUMN email VARCHAR(200) NULL AFTER technical_responsible',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
