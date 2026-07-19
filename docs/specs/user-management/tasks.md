# Tasks: Painel de Controle de Acessos

## Lote 1: Backend - Infraestrutura e Migração de Banco (Modelos)
- [x] Remover Entity `Permissao` obsoleta e utilizar Enum `Permissao`.
- [x] Criar entity `Perfil` (id, nome) com N:N para Enum `Permissao` (via `@ElementCollection`).
- [x] Atualizar entity `Usuario` (remover String role, adicionar N:1 para `Perfil`).
- [x] Adicionar Enum no código para Permissões Padrões (ex: `MANAGE_USERS`, `VIEW_DASHBOARD`, `VIEW_COMPLIANCE`).
- [x] Utilizar `DatabaseSeeder` para popular os dados e associar apenas o usuário `admin` ao perfil `ADMIN`. Expurgar contas legadas.
- [x] Criar migrações Flyway `V56` e `V57` para criar as tabelas e eliminar as constraints nativas de enum.
- [x] Atualizar `Usuario.getAuthorities()` para retornar os nomes das permissões ao invés da Role textual antiga.
- [x] Atualizar `TokenService` para inserir a nova lista de authorities no JWT.

## Lote 2: Backend - Controladores e Serviços (API REST)
- [x] Implementar `PerfilRepository`.
- [x] Criar `PerfilController` (CRUD) protegido por `@PreAuthorize("hasAuthority('MANAGE_USERS')")`.
- [x] Criar/Atualizar `UsuarioController` (CRUD) protegido por `@PreAuthorize("hasAuthority('MANAGE_USERS')")`.
- [x] Escrever Testes Unitários/Integração para os Controllers, incluindo validações do Motor de Apuração.

## Lote 3: Frontend - Telas de Gestão de Usuários
- [x] Criar rotas no React Router (`/admin/usuarios` e `/admin/perfis`).
- [x] Atualizar contexto de UI ou Zustand store para permissões do usuário logado (ler array de authorities do token e `me` endpoint).
- [x] Criar componente estilizado de Tabela de Usuários.
- [x] Criar Modal de Cadastro de Usuário (nome, login, email, senha temporária, perfil).
- [x] Criar tela/modal de Perfis com Checkboxes para Permissões Granulares baseadas no Enum.
- [x] Integrar UI com a API REST.
- [ ] Ajustar fluxo do MFA (Pendência no Lote 5 do Agente).
