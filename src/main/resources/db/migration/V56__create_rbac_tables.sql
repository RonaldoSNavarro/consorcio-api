CREATE TABLE perfis (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE perfil_permissoes (
    perfil_id BIGINT NOT NULL,
    permissao VARCHAR(50) NOT NULL,
    PRIMARY KEY (perfil_id, permissao),
    CONSTRAINT fk_perfil FOREIGN KEY (perfil_id) REFERENCES perfis(id) ON DELETE CASCADE
);

-- Insere o perfil padrão caso ele não exista e se o db estiver vazio. O seeder lidará com isso com mais segurança, mas o SQL ajuda.
-- Vamos permitir o Seeder fazer isso.

-- Adiciona a coluna perfil_id em usuarios e migra os dados
ALTER TABLE usuarios ADD COLUMN perfil_id BIGINT;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuario_perfil FOREIGN KEY (perfil_id) REFERENCES perfis(id);

-- Para manter compatibilidade com dados existentes (o admin), vamos precisar de um perfil admin.
-- O Seeder vai garantir a criação e atribuição.
-- Por enquanto, podemos remover a coluna role
ALTER TABLE usuarios DROP COLUMN role;
