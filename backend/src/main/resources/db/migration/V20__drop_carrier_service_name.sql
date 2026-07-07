-- =============================================================================
-- V20__drop_carrier_service_name.sql
--
-- Remove a coluna `service_name` da tabela `carriers`. O cadastro de
-- transportadoras agora contém apenas nome e status — o "serviço" deixou
-- de ser persistido.
--
-- Idempotente: `DROP COLUMN IF EXISTS` é tolerante a bases em que a coluna
-- já tenha sido removida (e.g., dev reinicializado).
-- =============================================================================

ALTER TABLE carriers
    DROP COLUMN IF EXISTS service_name;