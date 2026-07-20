-- Converte a coluna payment_condition de texto livre para o enum PaymentCondition.
-- Mapeia os display names existentes para os nomes dos enum values.
-- Valores que não correspondem a nenhum display name são convertidos para NULL.

ALTER TABLE accounts_receivable
    ADD COLUMN payment_condition_new VARCHAR(50) DEFAULT NULL AFTER payment_condition;

UPDATE accounts_receivable
SET payment_condition_new =
    CASE payment_condition
        -- À vista
        WHEN 'À Vista (Dinheiro)' THEN 'A_VISTA_DINHEIRO'
        WHEN 'PIX' THEN 'PIX'
        WHEN 'Boleto à Vista' THEN 'BOLETO_A_VISTA'
        -- Boleto
        WHEN 'Boleto 15 Dias' THEN 'BOLETO_15_DIAS'
        WHEN 'Boleto 28 Dias' THEN 'BOLETO_28_DIAS'
        WHEN 'Boleto 30 Dias' THEN 'BOLETO_30_DIAS'
        WHEN 'Boleto 45 Dias' THEN 'BOLETO_45_DIAS'
        WHEN 'Boleto 60 Dias' THEN 'BOLETO_60_DIAS'
        WHEN 'Boleto 90 Dias' THEN 'BOLETO_90_DIAS'
        -- Prazo único
        WHEN '7 Dias' THEN 'PRAZO_7_DIAS'
        WHEN '14 Dias' THEN 'PRAZO_14_DIAS'
        WHEN '15 Dias' THEN 'PRAZO_15_DIAS'
        WHEN '21 Dias' THEN 'PRAZO_21_DIAS'
        WHEN '28 Dias' THEN 'PRAZO_28_DIAS'
        WHEN '30 Dias' THEN 'PRAZO_30_DIAS'
        WHEN '45 Dias' THEN 'PRAZO_45_DIAS'
        WHEN '60 Dias' THEN 'PRAZO_60_DIAS'
        WHEN '90 Dias' THEN 'PRAZO_90_DIAS'
        -- Entrada + parcelas
        WHEN 'Entrada + 30 Dias' THEN 'ENTRADA_MAIS_30_DIAS'
        WHEN 'Entrada + 30 + 60 Dias' THEN 'ENTRADA_MAIS_30_60_DIAS'
        WHEN 'Entrada + 30 + 60 + 90 Dias' THEN 'ENTRADA_MAIS_30_60_90_DIAS'
        -- Parcelamento múltiplo
        WHEN '30/60 Dias' THEN 'PARCELAS_30_60'
        WHEN '30/60/90 Dias' THEN 'PARCELAS_30_60_90'
        WHEN '30/60/90/120 Dias' THEN 'PARCELAS_30_60_90_120'
        WHEN '15/30/45 Dias' THEN 'PARCELAS_15_30_45'
        WHEN '28/56/84 Dias' THEN 'PARCELAS_28_56_84'
        WHEN '30/45/60 Dias' THEN 'PARCELAS_30_45_60'
        WHEN '30/60/90/120/150 Dias' THEN 'PARCELAS_30_60_90_120_150'
        -- Faturado
        WHEN 'Faturado para 30 Dias' THEN 'FATURADO_30_DIAS'
        WHEN 'Faturado para 45 Dias' THEN 'FATURADO_45_DIAS'
        WHEN 'Faturado para 60 Dias' THEN 'FATURADO_60_DIAS'
        WHEN 'Faturado para 90 Dias' THEN 'FATURADO_90_DIAS'
        ELSE NULL
    END;

ALTER TABLE accounts_receivable
    DROP COLUMN payment_condition;

ALTER TABLE accounts_receivable
    CHANGE COLUMN payment_condition_new payment_condition VARCHAR(50) DEFAULT NULL;
