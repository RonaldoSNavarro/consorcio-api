# Contrato de API: Gerenciamento de Usuários e Acessos

## Endpoints

### 1. Permissões
- `GET /api/perfis/permissoes`
  - Auth: Requer `MANAGE_USERS`
  - Retorno: Lista de todas as permissões do sistema (Enum Strings).

### 2. Perfis (Roles)
- `GET /api/perfis`
  - Auth: Requer `MANAGE_ROLES`
  - Retorno: Lista de perfis com suas permissões atreladas.
- `POST /api/perfis`
  - Payload: `{ "nome": "VENDAS", "permissoes": ["VIEW_DASHBOARD", "MANAGE_COTAS"] }`
  - Auth: Requer `MANAGE_USERS`
- `PUT /api/perfis/{id}`
  - Payload: `{ "nome": "VENDAS", "permissoes": ["VIEW_DASHBOARD", "MANAGE_COTAS", "APPROVE_LANCE"] }`
  - Auth: Requer `MANAGE_USERS`
- `DELETE /api/perfis/{id}`
  - Auth: Requer `MANAGE_USERS`

### 3. Usuários
- `GET /api/usuarios`
  - Auth: Requer `MANAGE_USERS`
  - Retorno: Lista de usuários com nome, email, status MFA e Perfil atual.
- `POST /api/usuarios`
  - Payload: `{ "nome": "João", "login": "joao", "email": "joao@a.com", "senha": "...", "perfilId": 2 }`
  - Auth: Requer `MANAGE_USERS`
- `PUT /api/usuarios/{id}/perfil`
  - Payload: `{ "perfilId": 3 }`
  - Auth: Requer `MANAGE_USERS`
- `POST /api/usuarios/{id}/reset-password`
  - Auth: Requer `MANAGE_USERS`
  - Retorno: Nova senha temporária gerada ou email enviado.

### 4. Auth
- Payload JWT atualizado: Ao fazer o login com sucesso, o JWT deve incluir o array de permissões granulares e o nome do perfil. Ex:
  ```json
  {
    "sub": "admin",
    "role": "ADMIN",
    "authorities": ["MANAGE_USERS", "MANAGE_ROLES", "VIEW_DASHBOARD", "APPROVE_LANCE"]
  }
  ```
