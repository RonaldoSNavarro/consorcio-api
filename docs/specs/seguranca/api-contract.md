# Contrato de API de Autenticação e MFA

Este documento define o contrato REST da camada de segurança, autenticação e MFA (Multifator baseada em e-mail).

## 1. Endpoints de Sessão e Login

### `POST /api/login`
- **Descrição:** Realiza a autenticação primária utilizando usuário e senha da base de dados local.
- **Corpo da Requisição:** `{"login": "admin", "senha": "senha_do_usuario"}`
- **Respostas:**
  - **HTTP 200 OK**: Login bem-sucedido (se o MFA estiver desativado).
    - **Headers:** Define `Set-Cookie: token=<JWT_HMAC256>; HttpOnly; SameSite=Strict; Path=/; Max-Age=7200`
  - **HTTP 202 Accepted**: Requer segundo fator de autenticação (se o MFA estiver ativo). Dispara automaticamente o e-mail com o código.
    - **Corpo:** `{"mfaRequired": true, "tempToken": "<token_temporario_mfa>"}`
  - **HTTP 401 Unauthorized**: Credenciais inválidas.

### `POST /api/login/mfa-verify`
- **Descrição:** Valida o código de 6 dígitos enviado por e-mail para concluir o login.
- **Corpo da Requisição:** `{"code": "123456", "tempToken": "<token_temporario_mfa>"}`
- **Respostas:**
  - **HTTP 200 OK**: Login concluído.
    - **Headers:** Define `Set-Cookie: token=<JWT_HMAC256>; HttpOnly; SameSite=Strict; Path=/; Max-Age=7200`
  - **HTTP 400 Bad Request**: Código inválido ou expirado.

### `POST /api/login/logout`
- **Descrição:** Finaliza a sessão limpando o cookie seguro do navegador.
- **Resposta:** HTTP 200 OK
  - **Headers:** Define `Set-Cookie: token=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0`

### `GET /api/login/me`
- **Descrição:** Retorna os dados do usuário autenticado a partir do cookie/token.
- **Resposta:** HTTP 200 OK
  - **Corpo:** `{"login": "admin", "role": "ADMIN", "nome": "Administrador", "email": "ronaldoguitarrista@gmail.com", "mfaEnabled": true}`

---

## 2. Endpoints de Configuração de MFA

### `POST /api/mfa/setup`
- **Descrição:** Inicia a configuração do MFA, gerando e disparando o código de pareamento para o e-mail cadastrado.
- **Autenticação Requerida:** Sim
- **Resposta:** HTTP 200 OK

### `POST /api/mfa/confirm`
- **Descrição:** Confirma o código enviado por e-mail para ativar o MFA na conta do usuário.
- **Autenticação Requerida:** Sim
- **Corpo da Requisição:** `{"code": "123456"}`
- **Resposta:** HTTP 200 OK

### `POST /api/mfa/reset`
- **Descrição:** Desativa e limpa os códigos e status de MFA da conta do usuário.
- **Autenticação Requerida:** Sim
- **Resposta:** HTTP 200 OK
