-- Adiciona a coluna `base_unit_price` (preço unitário original, sem
-- margem de lucro) em itens de cotação e de proposta técnica, para
-- corrigir o bug em que a margem era reaplicada a cada edição.
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
-- A coluna é NOT NULL com default 0 e precisão igual ao `unit_price`.
-- Registros existentes (se houver) recebem `base_unit_price = 0` —
-- como a base de dados local foi limpa na fase de testes (DELETE de
-- todas as cotações/propostas/pedidos), a migration não precisa
-- recalcular retroativamente.

ALTER TABLE quotation_items
    ADD COLUMN base_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER unit_price;

ALTER TABLE technical_proposal_product_items
    ADD COLUMN base_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER unit_price;

ALTER TABLE technical_proposal_service_items
    ADD COLUMN base_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER price;

ALTER TABLE sales_order_items
    ADD COLUMN base_unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER unit_price;
