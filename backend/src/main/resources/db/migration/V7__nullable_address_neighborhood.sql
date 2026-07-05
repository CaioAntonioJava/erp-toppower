-- =============================================================================
-- V7__nullable_address_neighborhood.sql
--
-- Torna `address_neighborhood` (bairro) opcional em todas as tabelas que
-- embutem o Address embeddable.
--
-- Contexto:
--   O embeddable Address declarava `neighborhood` com `nullable = false`,
--   contradizendo seu próprio Javadoc ("Opcional"). A coluna era criada
--   NOT NULL pelo Hibernate (ddl-auto=update) em customers, companies e
--   suppliers, mas nenhum DTO, service ou form do frontend exigia o bairro
--   — resultando em INSERT que passava na validação de beans e falhava no
--   NOT NULL do banco. O `nullable = false` foi removido do embeddable; esta
--   migration reflete a mudança no schema existente.
--
--   `technical_proposals` já herda overrides permissivos (sem nullable=false)
--   e é incluída aqui por garantia/idempotência.
--
-- Idempotente:
--   `MODIFY COLUMN` redefine a coluna como nullable. Roda em todo boot
--   (spring.sql.init.mode=always) sem efeito colateral adicional.
-- =============================================================================

ALTER TABLE customers             MODIFY COLUMN address_neighborhood VARCHAR(100) NULL;
ALTER TABLE companies             MODIFY COLUMN address_neighborhood VARCHAR(100) NULL;
ALTER TABLE suppliers             MODIFY COLUMN address_neighborhood VARCHAR(100) NULL;
ALTER TABLE technical_proposals   MODIFY COLUMN address_neighborhood VARCHAR(100) NULL;