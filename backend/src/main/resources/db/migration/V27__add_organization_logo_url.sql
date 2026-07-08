-- Adiciona a coluna `logo_url` à tabela `organizations`, permitindo que
-- cada Organization (Top Power Engenharia, Top Power Materiais, ...)
-- tenha o seu próprio logo no cabeçalho dos PDFs de propostas,
-- cotações e pedidos de venda.
--
-- A coluna armazena o caminho público (relativo à raiz do backend)
-- servido pelo `WebMvcConfig` em `/logos/**` → `./uploads/logos/`.
-- Exemplo: `/logos/13.433.616-.../logo.png`.
--
-- IMPORTANTE — esta migration NÃO é executada pelo `spring.sql.init`
-- (não está listada em `application.properties`). A coluna é criada
-- automaticamente pelo Hibernate `ddl-auto=update` a partir da
-- anotação @Column na entidade Organization. Este arquivo é mantido
-- apenas como documentação do schema e para ser executado manualmente
-- em bancos que rodem com `ddl-auto=validate` (produção). Para rodar
-- manualmente no MySQL, use:
--
--   mysql -u root -p erp-toppower-api < V27__add_organization_logo_url.sql
--
-- (O `mysql` CLI interpreta `DELIMITER` corretamente; o
--  ResourceDatabasePopulator do Spring não.)

DROP PROCEDURE IF EXISTS v27_add_organizations_logo_url;
DELIMITER //
CREATE PROCEDURE v27_add_organizations_logo_url()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'organizations'
          AND COLUMN_NAME = 'logo_url'
    ) THEN
        ALTER TABLE organizations
            ADD COLUMN logo_url VARCHAR(500) NULL AFTER complement;
    END IF;
END //
DELIMITER ;
CALL v27_add_organizations_logo_url();
DROP PROCEDURE IF EXISTS v27_add_organizations_logo_url;