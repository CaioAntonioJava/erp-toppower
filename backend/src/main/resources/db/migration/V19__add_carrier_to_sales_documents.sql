-- =============================================================================
-- V19__add_carrier_to_sales_documents.sql
--
-- Adiciona a referência à transportadora (Carrier) nos três documentos
-- comerciais de vendas: quotations, technical_proposals e sales_orders.
--
-- Contexto:
--   O módulo Carrier existe isolado (cadastro admin-only). Agora os
--   formulários de Proposta Comercial, Proposta Técnica e Pedido de Venda
--   permitem selecionar a transportadora e seu serviço. A coluna
--   `carrier_id` é opcional (NULL) — nem todo documento precisa ter
--   transportadora vinculada.
--
--   Não há FK física (padrão do projeto: referências por UUID, sem
--   relacionamento JPA). A validação de existência da carrier é feita
--   no service quando o campo é informado.
--
-- Idempotência:
--   `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` só é suportado a partir
--   do MySQL 8.0.29. Para garantir compatibilidade com versões
--   anteriores (o MySQL local deste projeto é anterior), usamos o
--   mesmo padrão PREPARE/EXECUTE dinâmico da V18: a coluna só é
--   adicionada se ainda não existir em INFORMATION_SCHEMA.
-- =============================================================================

-- quotations
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quotations'
      AND COLUMN_NAME = 'carrier_id');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE quotations ADD COLUMN carrier_id BIGINT NULL AFTER freight_value',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- technical_proposals
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'technical_proposals'
      AND COLUMN_NAME = 'carrier_id');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE technical_proposals ADD COLUMN carrier_id BIGINT NULL AFTER delivery_type',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_orders
SET @has_col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sales_orders'
      AND COLUMN_NAME = 'carrier_id');
SET @sql = IF(@has_col = 0,
    'ALTER TABLE sales_orders ADD COLUMN carrier_id BIGINT NULL AFTER freight_value',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;