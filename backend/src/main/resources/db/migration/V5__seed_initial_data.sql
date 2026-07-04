-- =============================================================================
-- V5__seed_initial_data.sql
--
-- Migração de dados (seed) que reproduz no banco os mesmos registros que
-- existiam como mocks no frontend (src/mocks/*). Cria as seguintes linhas
-- quando as tabelas ainda estão vazias quanto a estas seeds (idempotente
-- via WHERE NOT EXISTS pelo uuid):
--   * 6 transportadoras (carriers)
--   * 12 empresas (companies)
--   * 12 clientes (customers)
--   * 12 vendedores (sellers)
--   * 12 produtos (products)
--   * 8 propostas (quotations) + seus 21 itens (quotation_items)
--
-- UUIDs são determinísticos (prefixo 00000000-0000-4000-8000-NNNNNNNNNNNN)
-- para que os relacionamentos entre quotations e customers/companies/sellers/
-- carriers/products possam ser referenciados. Carriers usam offset 2001..2006
-- para não colidir com as demais entidades (0001..0012), que vivem em tabelas
-- separadas. Itens de proposta usam a variante ...4000-9000-PPPPPPPIIIIII.
--
-- Executada a cada boot (spring.sql.init.mode=always), por isso é idempotente.
-- =============================================================================

-- Timestamp/autor padrão das seeds (todos os registros usam estes valores).
-- '2025-06-01 12:00:00' em UTC corresponde ao SEED_TIMESTAMP do mock.

-- =============================================================================
-- Carriers (6)
-- =============================================================================
INSERT INTO carriers (uuid, carrier_name, freight_value, status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000002001'), 'CORREIOS_SEDEX', 45.90, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM carriers WHERE uuid = UNHEX('00000000000040008000000000002001'));

INSERT INTO carriers (uuid, carrier_name, freight_value, status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000002002'), 'CORREIOS_PAC', 32.50, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM carriers WHERE uuid = UNHEX('00000000000040008000000000002002'));

INSERT INTO carriers (uuid, carrier_name, freight_value, status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000002003'), 'JADLOG', 58.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM carriers WHERE uuid = UNHEX('00000000000040008000000000002003'));

INSERT INTO carriers (uuid, carrier_name, freight_value, status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000002004'), 'OUTRAS_TRANSPORTADORAS', 70.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM carriers WHERE uuid = UNHEX('00000000000040008000000000002004'));

INSERT INTO carriers (uuid, carrier_name, freight_value, status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000002005'), 'CORREIOS_SEDEX', 50.00, 'INATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM carriers WHERE uuid = UNHEX('00000000000040008000000000002005'));

INSERT INTO carriers (uuid, carrier_name, freight_value, status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000002006'), NULL, 40.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM carriers WHERE uuid = UNHEX('00000000000040008000000000002006'));

-- =============================================================================
-- Companies (12)
-- Colunas de endereço: address_street, address_number, address_complement,
-- address_neighborhood, address_city, address_state, address_zip_code.
-- =============================================================================
INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000001'),
       'TOPPOWER ENERGIA E AUTOMACAO LTDA.', 'TOPPOWER', 'EMP000001', '11.223.300/0123-24',
       '110.042.490.117', FALSE, '6.123.456-7',
       'AV. PAULISTA', '1000', NULL, 'BELA VISTA', 'SAO PAULO', 'SP', '01310-500',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000001'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000002'),
       'VOLTA NACIONAL MATERIAIS ELETRICOS S.A.', 'VOLTA NACIONAL', 'EMP000002', '22.334.400/0124-25',
       '115.387.221.008', FALSE, '5.987.654-3',
       'RUA DAS INDUSTRIAS', '250', NULL, 'DISTRITO INDUSTRIAL', 'CURITIBA', 'PR', '81230-001',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000002'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000003'),
       'CABOS DO SUL DISTRIBUIDORA LTDA.', 'CABOS DO SUL', 'EMP000003', '33.445.500/0125-26',
       NULL, TRUE, NULL,
       'AV. DOS CABOS', '88', NULL, 'CAVALHADA', 'PORTO ALEGRE', 'RS', '91750-002',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000003'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000004'),
       'ILUMINAR PROJETOS E INSTALACOES LTDA.', 'ILUMINAR PROJETOS', 'EMP000004', '44.556.600/0126-27',
       '082.456.711.002', FALSE, '2.345.678-9',
       'RUA HALFELD', '1200', NULL, 'CENTRO', 'JUIZ DE FORA', 'MG', '36010-003',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000004'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000005'),
       'CENTELHA COMERCIO DE MATERIAIS ELETRICOS ME', 'CENTELHA MATERIAIS', 'EMP000005', '55.667.700/0127-28',
       NULL, TRUE, '8.765.432-1',
       'RUA HALFELD', '450', NULL, 'CENTRO', 'JUIZ DE FORA', 'MG', '36010-004',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000005'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000006'),
       'FORTE ENGENHARIA ELETRICA S.A.', 'FORTE ENGENHARIA', 'EMP000006', '66.778.800/0128-29',
       '130.554.881.119', FALSE, '3.210.987-6',
       'AV. AGAMENON MAGALHAES', '3450', NULL, 'BOA VIAGEM', 'RECIFE', 'PE', '50070-005',
       'INATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000006'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000007'),
       'CONDUTOR BRASILEIRO CABOS E FIOS LTDA.', 'CONDUTOR BRASILEIRO', 'EMP000007', '77.889.900/0129-20',
       '112.998.443.001', FALSE, '7.654.321-0',
       'ROD. ANHANGUERA', 'KM 312', NULL, 'DISTRITO INDUSTRIAL', 'RIBEIRAO PRETO', 'SP', '14070-006',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000007'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000008'),
       'PULSO AUTOMACAO INDUSTRIAL LTDA.', 'PULSO AUTOMACAO', 'EMP000008', '88.991.100/0130-75',
       '098.776.554.115', FALSE, '1.234.567-8',
       'AV. DOS IMIGRANTES', '1500', NULL, 'CENTRO', 'CAXIAS DO SUL', 'RS', '95020-007',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000008'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000009'),
       'REDE ELETRICA SERVICOS E MANUTENCAO ME', 'REDE ELETRICA', 'EMP000009', '99.112.200/0131-81',
       NULL, TRUE, '4.567.890-1',
       'RUA GOIAS', '320', NULL, 'CENTRO', 'GOIANIA', 'GO', '74010-008',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000009'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000010'),
       'JOULE MATERIAIS ELETRICOS LTDA.', 'JOULE MATERIAIS', 'EMP000010', '12.345.600/0132-30',
       '125.667.332.110', FALSE, '9.876.543-2',
       'AV. SETE DE SETEMBRO', '2200', NULL, 'CENTRO', 'SALVADOR', 'BA', '40080-009',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000010'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000011'),
       'AMPERE SOLUCOES EM ENERGIA LTDA.', 'AMPERE ENERGIA', 'EMP000011', '23.456.700/0133-30',
       NULL, TRUE, NULL,
       'SHN QUADRA 2', 'BLOCO A', NULL, 'ASA NORTE', 'BRASILIA', 'DF', '70702-001',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000011'));

INSERT INTO companies (uuid, legal_name, trade_name, code, cnpj, state_registration,
                       state_registration_exempt, municipal_registration,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000012'),
       'DELTA FORCA COMERCIAL ELETRICA S.A.', 'DELTA FORCA', 'EMP000012', '34.567.800/0134-31',
       '142.998.110.004', FALSE, '5.432.109-8',
       'AV. DOM LUIS', '1300', NULL, 'ALDEOTA', 'FORTALEZA', 'CE', '60110-002',
       'INATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE uuid = UNHEX('00000000000040008000000000000012'));

-- =============================================================================
-- Customers (12) — pessoa física (BasePerson: name, email, phone, cpf)
-- =============================================================================
INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000001'),
       'MARIA DAS GRACAS SILVA', 'maria.silva@example.com', '(11) 99887-7654', '111.222.333-96', 'CLI000001',
       'RUA DAS ACACIAS', '120', NULL, 'JARDIM AMERICA', 'SAO PAULO', 'SP', '01230-401',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000001'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000002'),
       'JOAO CARLOS PEREIRA', 'joao.pereira@example.com', '(21) 99771-2345', '123.456.789-09', 'CLI000002',
       'AV. BRASIL', '540', NULL, 'FUNCIONARIOS', 'BELO HORIZONTE', 'MG', '30110-002',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000002'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000003'),
       'ANA PAULA FERNANDES', 'ana.fernandes@example.com', '(31) 99559-8765', '234.567.890-92', 'CLI000003',
       'RUA VOLUNTARIOS DA PATRIA', '880', NULL, 'CENTRO', 'PORTO ALEGRE', 'RS', '90020-003',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000003'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000004'),
       'PEDRO HENRIQUE SOUZA', 'pedro.souza@example.com', '(41) 99114-4444', '345.678.901-75', 'CLI000004',
       'RUA XV DE NOVEMBRO', '200', NULL, 'CENTRO', 'CURITIBA', 'PR', '80030-004',
       'INATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000004'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000005'),
       'CAMILA RODRIGUES DE OLIVEIRA', 'camila.oliveira@example.com', '(51) 99990-1234', '456.789.012-49', 'CLI000005',
       'AV. PAULISTA', '2300', NULL, 'BELA VISTA', 'SAO PAULO', 'SP', '01310-005',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000005'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000006'),
       'LUCAS ALMEIDA MARTINS', 'lucas.martins@example.com', '(61) 99335-6789', '567.890.123-03', 'CLI000006',
       'RUA DOS PINHEIROS', '450', NULL, 'PINHEIROS', 'SAO PAULO', 'SP', '05420-006',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000006'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000007'),
       'FERNANDA CASTRO LIMA', 'fernanda.lima@example.com', '(71) 99888-8765', '678.901.234-69', 'CLI000007',
       'AV. AGAMENON MAGALHAES', '1200', NULL, 'BOA VIAGEM', 'RECIFE', 'PE', '50070-007',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000007'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000008'),
       'RAFAEL MENDES DOS SANTOS', 'rafael.santos@example.com', '(81) 99661-2345', '789.012.345-05', 'CLI000008',
       'RUA DA PRAIA', '33', NULL, 'CENTRO HISTORICO', 'PORTO ALEGRE', 'RS', '90010-008',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000008'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000009'),
       'JULIANA RIBEIRO DA COSTA', 'juliana.costa@example.com', '(91) 99770-0111', '890.123.456-42', 'CLI000009',
       'AV. GOIAS', '800', NULL, 'SETOR CENTRAL', 'GOIANIA', 'GO', '74020-009',
       'INATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000009'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000010'),
       'BRUNO HENRIQUE BARBOSA', 'bruno.barbosa@example.com', '(12) 99552-2222', '901.234.567-70', 'CLI000010',
       'AV. SETE DE SETEMBRO', '1100', NULL, 'CENTRO', 'SALVADOR', 'BA', '40010-001',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000010'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000011'),
       'PATRICIA NOGUEIRA VIEIRA', 'patricia.vieira@example.com', '(22) 99449-9999', '135.791.357-59', 'CLI000011',
       'RUA PADRE ANCHIETA', '2500', NULL, 'BIGORRILHO', 'CURITIBA', 'PR', '80730-002',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000011'));

INSERT INTO customers (uuid, name, email, phone, cpf, code,
                       address_street, address_number, address_complement,
                       address_neighborhood, address_city, address_state, address_zip_code,
                       status, created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000012'),
       'GUSTAVO LIMA DE ARAUJO', 'gustavo.araujo@example.com', '(32) 99117-7777', '246.802.468-04', 'CLI000012',
       'AV. DOM LUIS', '700', NULL, 'ALDEOTA', 'FORTALEZA', 'CE', '60110-003',
       'ATIVO', '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE uuid = UNHEX('00000000000040008000000000000012'));

-- =============================================================================
-- Sellers (12) — BasePerson: name, email, phone, cpf + commission_rate
-- =============================================================================
INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000001'),
       'CARLOS EDUARDO MENDES', 'carlos.mendes@toppower.local', '(11) 98123-4567', '112.334.455-88', 3.50, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000001'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000002'),
       'RENATA LOPES CARVALHO', 'renata.carvalho@toppower.local', '(11) 99555-5555', '122.334.455-03', 2.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000002'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000003'),
       'MARCELO AUGUSTO REIS', 'marcelo.reis@toppower.local', '(21) 98121-1111', '132.334.455-12', 5.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000003'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000004'),
       'TATIANA VIEIRA BORGES', 'tatiana.borges@toppower.local', '(31) 97778-8888', '142.334.455-30', 4.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000004'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000005'),
       'DIEGO SOARES DE FREITAS', 'diego.freitas@toppower.local', '(41) 96669-9999', '152.334.455-57', 1.50, 'INATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000005'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000006'),
       'LARISSA GONCALVES PINTO', 'larissa.pinto@toppower.local', '(51) 95550-1234', '162.334.455-74', 6.50, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000006'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000007'),
       'FABIO AUGUSTO NOGUEIRA', 'fabio.nogueira@toppower.local', '(61) 94445-6789', '172.334.455-91', 2.50, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000007'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000008'),
       'VANESSA DUARTE MOREIRA', 'vanessa.moreira@toppower.local', '(71) 93330-0001', '182.334.455-09', 3.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000008'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000009'),
       'EDUARDO SALLES TEIXEIRA', 'eduardo.teixeira@toppower.local', '(81) 92229-9888', '192.334.455-26', 7.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000009'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000010'),
       'BEATRIZ AMARAL SALES', 'beatriz.sales@toppower.local', '(91) 91117-7776', '202.334.455-79', NULL, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000010'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000011'),
       'HENRIQUE PACHECO BRANDAO', 'henrique.brandao@toppower.local', '(12) 99996-6665', '212.334.455-96', 4.50, 'INATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000011'));

INSERT INTO sellers (uuid, name, email, phone, cpf, commission_rate, status,
                     created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000012'),
       'ISABELA CORDEIRO MAIA', 'isabela.maia@toppower.local', '(22) 98884-4443', '222.334.455-03', 10.00, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM sellers WHERE uuid = UNHEX('00000000000040008000000000000012'));

-- =============================================================================
-- Products (12)
-- stock_quantity tem 4 casas decimais.
-- =============================================================================
INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000001'),
       'CABO FLEXIVEL PP 2X2,5 MM² 750V', 'CAB-PP-2X2.5', 'METROS', 6.90, 1500.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000001'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000002'),
       'CABO FLEXIVEL PP 3X2,5 MM² 750V', 'CAB-PP-3X2.5', 'METROS', 9.50, 980.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000002'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000003'),
       'CABO RIGIDO 750V 4 MM² (ROLO 100M)', 'CAB-RIG-4-100', 'BOBINA', 489.90, 24.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000003'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000004'),
       'CABO COAXIAL RG6 75 OHM (ROLO 100M)', 'CAB-COX-RG6-100', 'BOBINA', 219.50, 12.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000004'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000005'),
       'DISJUNTOR MONOPOLAR 20A CURVA C', 'DISJ-MON-20C', 'UNIDADE', 12.90, 320.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000005'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000006'),
       'DISJUNTOR TRIPOLAR 63A CURVA C', 'DISJ-TRI-63C', 'UNIDADE', 119.90, 75.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000006'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000007'),
       'TOMADA 2P+T 20A BRANCA', 'TOM-2PT-20-BR', 'UNIDADE', 8.50, 540.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000007'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000008'),
       'INTERRUPTOR SIMPLES 10A BRANCO', 'INT-SIM-10-BR', 'UNIDADE', 6.20, 610.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000008'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000009'),
       'QUADRO DE DISTRIBUICAO 12 DISJUNTORES SOBREPOR', 'QD-12-SOB', 'UNIDADE', 145.00, 38.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000009'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000010'),
       'ELETRODUTO PVC RIGIDO 3/4" (BARRA 3M)', 'ELE-PVC-3/4-3M', 'UNIDADE', 18.90, 220.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000010'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000011'),
       'LAMPADA LED BULBO 9W 6500K BIVOLT', NULL, 'UNIDADE', 11.90, 480.0000, 'ATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000011'));

INSERT INTO products (uuid, name, code, unit_type, price, stock_quantity, status,
                      created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000012'),
       'CABO DE COBRE NU 50 MM² (ROLO 50M)', 'CAB-NU-50-50', 'BOBINA', 1890.00, 4.0000, 'INATIVO',
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM products WHERE uuid = UNHEX('00000000000040008000000000000012'));

-- =============================================================================
-- Quotations (8) — number inicia em 1500.
-- customer_uuid e company_uuid são mutuamente exclusivos. carrier_uuid,
-- freight_type, freight_value, discount_type, discount, attention, notes,
-- validity_days e payment_condition variam por proposta.
-- profit_margin é NOT NULL (0 em todas as seeds).
-- =============================================================================
-- Proposta #1 (CUSTOMER, Maria das Graças, seller Carlos, carrier SEDEX)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000001'), 1500, '2026-06-15',
       UNHEX('00000000000040008000000000000001'), NULL, 'SR. MARCOS',
       UNHEX('00000000000040008000000000000001'), 'PERCENT', 5.00, 15, 'PIX',
       'Entrega em 5 dias uteis. Garantia de 1 ano.', 'ATIVA',
       UNHEX('00000000000040008000000000002001'), 'CIF', 45.90, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000001'));

-- Proposta #2 (COMPANY, TopPower, seller Renata, carrier JADLOG)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000002'), 1501, '2026-06-20',
       NULL, UNHEX('00000000000040008000000000000001'), NULL,
       UNHEX('00000000000040008000000000000002'), 'AMOUNT', 250.00, 30, 'BOLETO_30_DIAS',
       'Instalacao industrial — volumes fracionados conforme cronograma.', 'ATIVA',
       UNHEX('00000000000040008000000000002003'), 'CIF', 58.00, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000002'));

-- Proposta #3 (CUSTOMER, João Carlos, seller Marcelo, sem frete)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000003'), 1502, '2026-06-25',
       UNHEX('00000000000040008000000000000002'), NULL, 'ENG. JOAO',
       UNHEX('00000000000040008000000000000003'), NULL, NULL, 10, 'A_VISTA_DINHEIRO',
       NULL, 'ATIVA',
       NULL, NULL, NULL, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000003'));

-- Proposta #4 (COMPANY, Iluminar, seller Carlos, EXPIRADA, sem frete)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000004'), 1503, '2026-04-10',
       NULL, UNHEX('00000000000040008000000000000004'), NULL,
       UNHEX('00000000000040008000000000000001'), NULL, NULL, 30, 'PARCELAS_30_60',
       'Proposta expirada — cliente nao respondeu no prazo.', 'EXPIRADA',
       NULL, NULL, NULL, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000004'));

-- Proposta #5 (CUSTOMER, Camila, seller Renata, CONVERTIDA, sem frete)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000005'), 1504, '2026-05-12',
       UNHEX('00000000000040008000000000000005'), NULL, NULL,
       UNHEX('00000000000040008000000000000002'), NULL, NULL, 15, 'PIX',
       'Convertida em pedido — ver OS #4521.', 'CONVERTIDA',
       NULL, NULL, NULL, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000005'));

-- Proposta #6 (COMPANY, Condutor Brasileiro, seller Marcelo, CANCELADA, sem frete)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000006'), 1505, '2026-05-28',
       NULL, UNHEX('00000000000040008000000000000007'), NULL,
       UNHEX('00000000000040008000000000000003'), NULL, NULL, 7, 'BOLETO_28_DIAS',
       'Cancelada — cliente solicitou revisao de preco.', 'CANCELADA',
       NULL, NULL, NULL, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000006'));

-- Proposta #7 (CUSTOMER, Fernanda, seller Carlos, desconto global 3%)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000007'), 1506, '2026-06-30',
       UNHEX('00000000000040008000000000000007'), NULL, 'SRA. FERNANDA',
       UNHEX('00000000000040008000000000000001'), 'PERCENT', 3.00, 20, 'ENTRADA_MAIS_30_60_DIAS',
       NULL, 'ATIVA',
       NULL, NULL, NULL, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000007'));

-- Proposta #8 (COMPANY, Joule Materiais, seller Renata, carrier PAC)
INSERT INTO quotations (uuid, number, issue_date, customer_uuid, company_uuid, attention,
                        seller_uuid, discount_type, discount, validity_days, payment_condition,
                        notes, status, carrier_uuid, freight_type, freight_value, profit_margin,
                        created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040008000000000000008'), 1507, '2026-07-01',
       NULL, UNHEX('00000000000040008000000000000010'), NULL,
       UNHEX('00000000000040008000000000000002'), NULL, NULL, 45, 'FATURADO_45_DIAS',
       'Atendimento a obra no interior — frete CIF.', 'ATIVA',
       UNHEX('00000000000040008000000000002002'), 'FOB', 32.50, 0.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotations WHERE uuid = UNHEX('00000000000040008000000000000008'));

-- =============================================================================
-- Quotation items (21) — UUIDs na variante ...4000-9000-PPPPPPPPPPPP (P=proposta, I=item)
-- total_price = (unit_price * quantity) - desconto da linha (ja calculado).
-- =============================================================================
-- Proposta #1 — 2 itens
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000001000001'), UNHEX('00000000000040008000000000000001'),
       UNHEX('00000000000040008000000000000001'), 50.0000, 6.90, NULL, NULL, 345.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000001000001'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000001000002'), UNHEX('00000000000040008000000000000001'),
       UNHEX('00000000000040008000000000000007'), 4.0000, 8.50, NULL, NULL, 34.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000001000002'));

-- Proposta #2 — 4 itens
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000002000001'), UNHEX('00000000000040008000000000000002'),
       UNHEX('00000000000040008000000000000003'), 6.0000, 489.90, NULL, NULL, 2939.40,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000002000001'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000002000002'), UNHEX('00000000000040008000000000000002'),
       UNHEX('00000000000040008000000000000005'), 12.0000, 12.90, NULL, NULL, 154.80,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000002000002'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000002000003'), UNHEX('00000000000040008000000000000002'),
       UNHEX('00000000000040008000000000000006'), 4.0000, 119.90, NULL, NULL, 479.60,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000002000003'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000002000004'), UNHEX('00000000000040008000000000000002'),
       UNHEX('00000000000040008000000000000009'), 2.0000, 145.00, NULL, NULL, 290.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000002000004'));

-- Proposta #3 — 2 itens
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000003000001'), UNHEX('00000000000040008000000000000003'),
       UNHEX('00000000000040008000000000000002'), 80.0000, 9.50, NULL, NULL, 760.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000003000001'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000003000002'), UNHEX('00000000000040008000000000000003'),
       UNHEX('00000000000040008000000000000010'), 12.0000, 18.90, NULL, NULL, 226.80,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000003000002'));

-- Proposta #4 — 2 itens
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000004000001'), UNHEX('00000000000040008000000000000004'),
       UNHEX('00000000000040008000000000000011'), 100.0000, 11.90, NULL, NULL, 1190.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000004000001'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000004000002'), UNHEX('00000000000040008000000000000004'),
       UNHEX('00000000000040008000000000000008'), 30.0000, 6.20, NULL, NULL, 186.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000004000002'));

-- Proposta #5 — 2 itens
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000005000001'), UNHEX('00000000000040008000000000000005'),
       UNHEX('00000000000040008000000000000004'), 2.0000, 219.50, NULL, NULL, 439.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000005000001'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000005000002'), UNHEX('00000000000040008000000000000005'),
       UNHEX('00000000000040008000000000000011'), 20.0000, 11.90, NULL, NULL, 238.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000005000002'));

-- Proposta #6 — 1 item
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000006000001'), UNHEX('00000000000040008000000000000006'),
       UNHEX('00000000000040008000000000000012'), 10.0000, 1890.00, NULL, NULL, 18900.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000006000001'));

-- Proposta #7 — 2 itens (item 1 tem desconto de linha PERCENT 10%)
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000007000001'), UNHEX('00000000000040008000000000000007'),
       UNHEX('00000000000040008000000000000001'), 30.0000, 6.90, 'PERCENT', 10.00, 186.30,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000007000001'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000007000002'), UNHEX('00000000000040008000000000000007'),
       UNHEX('00000000000040008000000000000005'), 8.0000, 12.90, NULL, NULL, 103.20,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000007000002'));

-- Proposta #8 — 4 itens
INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000008000001'), UNHEX('00000000000040008000000000000008'),
       UNHEX('00000000000040008000000000000006'), 20.0000, 119.90, NULL, NULL, 2398.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000008000001'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000008000002'), UNHEX('00000000000040008000000000000008'),
       UNHEX('00000000000040008000000000000009'), 5.0000, 145.00, NULL, NULL, 725.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000008000002'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000008000003'), UNHEX('00000000000040008000000000000008'),
       UNHEX('00000000000040008000000000000010'), 50.0000, 18.90, NULL, NULL, 945.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000008000003'));

INSERT INTO quotation_items (uuid, quotation_uuid, product_uuid, quantity, unit_price,
                              discount_type, discount, total_price,
                              created_at, updated_at, created_by, updated_by)
SELECT UNHEX('00000000000040009000000008000004'), UNHEX('00000000000040008000000000000008'),
       UNHEX('00000000000040008000000000000007'), 30.0000, 8.50, NULL, NULL, 255.00,
       '2025-06-01 12:00:00', '2025-06-01 12:00:00', 'seed@toppower.local', NULL
WHERE NOT EXISTS (SELECT 1 FROM quotation_items WHERE uuid = UNHEX('00000000000040009000000008000004'));