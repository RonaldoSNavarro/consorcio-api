# Spec: Gerenciamento de Usuários e Controle de Acesso (RBAC)

## Visão Geral
Esta capability substitui o controle de acesso atual (onde `role` é apenas uma String na tabela `usuarios`) por um modelo RBAC dinâmico baseado em Perfis (Roles) e Permissões (Privileges). O painel administrativo permitirá gerenciar quem tem acesso ao quê.

## Arquitetura Definida (via Grill-me)
- **Permissões Granulares (Privileges):** São **fixas** no código através do Enum `Permissao` (ex: `MANAGE_USERS`, `VIEW_DASHBOARD`, `VIEW_COMPLIANCE`). A fonte da verdade para o `@PreAuthorize` é o Java. Elas são gravadas via `@ElementCollection`.
- **Perfis (Roles):** São entidades dinâmicas (`Perfil`) que agrupam Permissões (relacionamento N:N com o Enum `Permissao`). No startup da aplicação, o `DatabaseSeeder` cria o perfil `ADMIN` associado a todas as permissões.
- **Usuários:** Relacionamento N:1 com Perfil (cada usuário tem um perfil). Apenas a conta `admin` é criada por padrão (as contas legacy como gestor e compliance foram expurgadas em prol do novo RBAC).

## Histórias de Usuário / Requisitos

### REQ-USR-01: Migração do Esquema Atual
- **Given** o sistema possui uma tabela `usuarios` com coluna `role`
- **When** o Flyway rodar as migrações (V56 e V57)
- **Then** criará as tabelas `perfis` e `perfil_permissoes` (para o Enum), e alterará `usuarios` para referenciar `perfil_id`. O perfil `ADMIN` será criado com todas as permissões do sistema. Usuários antigos serão deletados, deixando apenas o Admin padrão.

### REQ-USR-02: Gerenciar Perfis
- **Given** que sou um usuário com permissão `MANAGE_ROLES`
- **When** acesso a tela de Perfis no Painel Admin
- **Then** posso listar, criar, editar e excluir Perfis, e "ticar" quais Permissões aquele Perfil possui.

### REQ-USR-03: Gerenciar Usuários
- **Given** que sou um usuário com permissão `MANAGE_USERS`
- **When** acesso a tela de Usuários
- **Then** posso listar, cadastrar novos usuários, resetar senhas e associar o usuário a um Perfil existente.

### REQ-USR-04: Autenticação Segura (Spring Security)
- **Given** que o usuário realiza o login e valida o MFA
- **When** o token JWT for gerado
- **Then** ele deve incluir a lista de permissões (`authorities`) no payload do token para que o frontend possa esconder/mostrar botões e o backend proteja as rotas.

### REQ-USR-05: Proteção IDOR e Permissões Globais
- **Given** um gestor ou usuário com permissões globais (`VIEW_CLIENTES`, `MANAGE_CLIENTES`, `VIEW_COTAS`, `MANAGE_COTAS`)
- **When** ele tenta acessar ou modificar dados de clientes ou cotas que não pertencem ao seu usuário
- **Then** a proteção contra *Insecure Direct Object Reference (IDOR)* na camada de serviço validará suas *authorities* globais do Spring Security. Se possuir a autoridade, a ação é permitida. Caso contrário, a ação é barrada com acesso negado (permitindo acesso estrito apenas ao próprio "dono" dos dados).
