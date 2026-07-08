-- Adiciona a coluna `base_unit_price` (preço unitário original, sem
-- margem de lucro) em itens de cotação, de proposta técnica (produto
-- e serviço) e de pedido de venda, para corrigir o bug em que a margem
-- era reaplicada a cada edição.
--
-- Contexto: a partir da refatoração da margem por item, o `unit_price`
-- persistido passou a ser o valor JÁ COM a margem de lucro aplicada
-- (snapshot final). Ao reabrir uma proposta para edição, o frontend
-- carregava esse `unit_price` como preço base e reenviava no payload,
-- fazendo o backend aplicar a margem de novo (duplicando o valor).
--
-- Solução: armazenar também o preço original (`base_unit_price`) enviado
-- pelo usuário, sem margem. O `unit_price` continua sendo o snapshot
-- final (com margem) para fins de exibição/histórico, mas o frontend
-- passa a enviar e editar sempre o `base_unit_price`.
--
-- IMPORTANTE — esta migration NÃO é executada pelo `spring.sql.init`
-- (não está listada em `application.properties`). As colunas são criadas
-- automaticamente pelo Hibernate `ddl-auto=update` a partir das
-- anotações @Column nas entidades. Este arquivo é mantido apenas como
-- documentação do schema e para ser executado manualmente em bancos que
-- rodem com `ddl-auto=validate` (produção). Para rodar manualmente no
-- MySQL, use:
--
--   mysql -u root -p erp-toppower-api < V26__add_base_unit_price_to_sales_items.sql
--
-- (O `mysql` CLI interpreta `DELIMITER` corretamente; o
--  ResourceDatabasePopulator do Spring não.)

-- quotation_items.base_unit_price
DROP PROCEDURE IF EXISTS v26_add_quotation_items_base_unit_price;
DELIMITER //
CREATE PROCEDURE v26_add_quotation_items_base_unit_price()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'quotation_items'
          AND COLUMN_NAME = 'base_unit_price'
    ) THEN
        ALTER TABLE quotation_items
            ADD COLUMN base_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER unit_price;
    END IF;
END //
DELIMITER ;
CALL v26_add_quotation_items_base_unit_price();
DROP PROCEDURE IF EXISTS v26_add_quotation_items_base_unit_price;

-- technical_proposal_product_items.base_unit_price
DROP PROCEDURE IF EXISTS v26_add_tp_product_items_base_unit_price;
DELIMITER //
CREATE PROCEDURE v26_add_tp_product_items_base_unit_price()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'technical_proposal_product_items'
          AND COLUMN_NAME = 'base_unit_price'
    ) THEN
        ALTER TABLE technical_proposal_product_items
            ADD COLUMN base_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER unit_price;
    END IF;
END //
DELIMITER ;
CALL v26_add_tp_product_items_base_unit_price();
DROP PROCEDURE IF EXISTS v26_add_tp_product_items_base_unit_price;

-- technical_proposal_service_items.base_price
DROP PROCEDURE IF EXISTS v26_add_tp_service_items_base_price;
DELIMITER //
CREATE PROCEDURE v26_add_tp_service_items_base_price()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'technical_proposal_service_items'
          AND COLUMN_NAME = 'base_price'
    ) THEN
        ALTER TABLE technical_proposal_service_items
            ADD COLUMN base_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER price;
    END IF;
END //
DELIMITER ;
CALL v26_add_tp_service_items_base_price();
DROP PROCEDURE IF EXISTS v26_add_tp_service_items_base_price;

-- sales_order_items.base_unit_price
DROP PROCEDURE IF EXISTS v26_add_sales_order_items_base_unit_price;
DELIMITER //
CREATE PROCEDURE v26_add_sales_order_items_base_unit_price()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sales_order_items'
          AND COLUMN_NAME = 'base_unit_price'
    ) THEN
        ALTER TABLE sales_order_items
            ADD COLUMN base_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER unit_price;
    END IF;
END //
DELIMITER ;
CALL v26_add_sales_order_items_base_unit_price();
DROP PROCEDURE IF EXISTS v26_add_sales_order_items_base_unit_price;