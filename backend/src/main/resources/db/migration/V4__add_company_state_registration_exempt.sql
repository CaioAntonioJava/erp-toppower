-- Migration V4: adicionar coluna `state_registration_exempt` (IE Isento) à tabela `companies`.
--
-- Contexto:
--   Algumas empresas são dispensadas de Inscrição Estadual (IE) — caso
--   previsto na legislação fiscal brasileira para MEIs, prestadores de
--   serviço, empresas em início de atividade, etc. O sistema precisa
--   representar essa informação para evitar a cobrança de IE no momento
--   da emissão de documentos fiscais.
--
--   A coluna é um BOOLEAN (representado como TINYINT(1) no MySQL),
--   segue o mesmo padrão dos outros campos booleanos do schema, e tem
--   default `FALSE` para empresas pré-existentes (assume-se que, por
--   padrão, a empresa NÃO é isenta — caso seja, o usuário edita o
--   cadastro e marca o checkbox).
--
--   Hibernate com `ddl-auto=update` criaria a coluna automaticamente,
--   mas a criação manual via migration garante reprodutibilidade em
--   todos os ambientes e versionamento adequado via Flyway.
--
--   O bloco abaixo torna a migration idempotente: se o Hibernate já
--   tiver criado a coluna antes desta migration rodar (cenário comum
--   em dev com `ddl-auto=update`), o ALTER TABLE é pulado em vez de
--   quebrar com erro "Duplicate column". Funciona em qualquer MySQL
--   ≥ 5.5 / MariaDB, e dispensa o `ADD COLUMN IF NOT EXISTS`
--   introduzido apenas no MySQL 8.0.29.

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'companies'
      AND COLUMN_NAME = 'state_registration_exempt'
);

SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE companies ADD COLUMN state_registration_exempt BOOLEAN NOT NULL DEFAULT FALSE AFTER state_registration',
    'DO 0'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
