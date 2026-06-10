-- FC-04 FIX: Adiciona coluna de role/perfil ao usuário para controle RBAC granular.
-- Perfis possíveis: ADMIN (administrador do consórcio), CONSORCIADO (cliente final), AUDITOR (somente leitura)
ALTER TABLE usuarios ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN';
