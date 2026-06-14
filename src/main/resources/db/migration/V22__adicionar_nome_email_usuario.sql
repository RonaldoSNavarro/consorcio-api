-- Migration Sprint 4: Adiciona colunas nome e email à tabela usuarios para enriquecer o payload do /me

ALTER TABLE usuarios ADD COLUMN nome VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN email VARCHAR(100);

-- Popula os dados iniciais dos usuários existentes
UPDATE usuarios SET nome = 'Administrador', email = 'admin@consorcio.com.br' WHERE login = 'admin';
UPDATE usuarios SET nome = 'Gestor', email = 'gestor@consorcio.com.br' WHERE login = 'gestor';
UPDATE usuarios SET nome = 'Consorciado', email = 'consorciado@consorcio.com.br' WHERE login = 'consorciado';
UPDATE usuarios SET nome = login, email = login || '@consorcio.com.br' WHERE nome IS NULL;
