-- Migration to insert OFAC list entry and compliance alerts for OSAMA BIN LADEN
INSERT INTO listas_restritivas (id, nome, documento_origem, origem, data_inclusao)
SELECT 100, 'LISTA OFAC', 'OFAC SDN LIST', 'OFAC', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM listas_restritivas WHERE id = 100
);

INSERT INTO alertas_compliance (cliente_id, lista_id, score, status, justificativa, data_deteccao)
SELECT c.id, 100, 1.0000, 'CONFIRMADO', 'Cliente consta na lista OFAC (PLD/FT).', CURRENT_TIMESTAMP
FROM clientes c
WHERE c.cpf_cnpj IN ('00011122233', '00011122299')
  AND NOT EXISTS (
      SELECT 1 FROM alertas_compliance ac 
      WHERE ac.cliente_id = c.id AND ac.lista_id = 100
  );
