# Especificação do Domínio: Autenticação, Sessão e Segurança

## Visão Geral
Este módulo trata do controle de acesso, geração e validação de tokens JWT via Cookies e políticas globais de Cross-Origin (CORS).

## Regras de Negócio e Segurança

### 1. Cookies HttpOnly e Secure
- O token JWT é entregue via um cookie `HttpOnly`, `Secure` (configurado via `api.security.cookie.secure` no `application.properties`) e `SameSite=Strict`. O payload JSON do login não expõe o token para mitigar roubo via XSS. O logout exclui o cookie ativamente seguindo as mesmas restrições.

### 2. CORS e Cross-Origin
- As origens (Origins) de front-end permitidas são carregadas estaticamente através da propriedade `api.cors.allowed-origins` em produção (via `application.properties` ou injetado por variável de ambiente).
- Todas as operações CRUD expõem o header `Authorization` e permitem credentials (para envio automático do cookie).

### 3. Recuperação de Sessão Ativa
- Para resiliência do SPA, o frontend consulta `/api/login/me` periodicamente. O interceptador Spring Security utiliza o Cookie validado para reidratar o contexto do usuário (State Rehydration).

### 4. Mascaramento Dinâmico de Dados (Defesa em Profundidade) - RN-AUTH-004
- **Intrusion Detection:** Para mitigar exfiltração em massa (Data Breach) ou *Scraping* em caso de contas comprometidas (ex: phishing), o sistema implementa um filtro de Rate Limiting e detecção de anomalias (ex: mais de 30 requisições por minuto da mesma sessão/IP).
- **Graceful Degradation (Modo Defensivo):** Quando uma anomalia é detectada, a sessão é classificada como "Suspeita". O backend não derruba a conexão, mas aciona dinamicamente a ofuscação (máscara LGPD) nos DTOs que devolvem dados sensíveis (ex: CPF, Nome, Email, Telefone do Cliente). Operadores legítimos verão os dados abertos, atacantes/robôs verão apenas asteriscos.
