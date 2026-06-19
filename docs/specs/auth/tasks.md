# 📋 Decomposição de Tarefas — Autenticação e Sessão (auth)

*   **Capability**: auth
*   **Spec de referência**: [spec.md](spec.md)
*   **Total de tarefas**: 8
*   **REQ-IDs cobertos**: 2/2

---

## Tarefas

### [BACKEND] REQ-AUTH-001: Autenticação Stateless e Emissão de Token
- [x] Criar entidade `Usuario.java` com campos login, senha, role, nome, email
- [x] Criar `AutenticacaoService.java` — lógica de autenticação via Spring Security
- [x] Criar `TokenService.java` — geração e validação de JWT
- [x] Criar `AutenticacaoController.java` — endpoint `POST /api/login` com cookie HttpOnly
- [x] Criar `AutenticacaoController.logout()` — endpoint `POST /api/login/logout` com invalidação de cookie
- [x] Criar DTOs: `DadosAutenticacao`, `DadosTokenJWT`

### [BACKEND] REQ-AUTH-002: Revalidação Ativa de Sessão (F5-safety)
- [x] Criar `AutenticacaoController.obterUsuarioLogado()` — endpoint `GET /api/login/me`
- [x] Criar DTO `DadosUsuarioLogado` com login, role, nome, email
