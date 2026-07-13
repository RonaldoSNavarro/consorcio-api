# Tarefas Lote 1 - Fundação: Keycloak + Resource Server

Este arquivo registra a execução das tarefas estruturais (Lote 1) para a migração OAuth2/Keycloak.

## 1. Tarefas Concluídas
- [x] **Infraestrutura Docker:** Atualização do `docker-compose.yml` e `Dockerfile` para expor a API na porta `8081` (evitando colisão de porta host) e adicionar o serviço `keycloak` na porta `8180`.
- [x] **Configuração Keycloak:** Criação do arquivo de importação inicial do realm (`keycloak/realm-consorcio.json`) com clients `consorcio-api`, `consorcio-frontend` e 5 usuários de teste.
- [x] **Integração Backend OAuth2:** Inserção do pacote `spring-boot-starter-oauth2-resource-server` no `pom.xml`.
- [x] **Mapeamento Spring properties:** Adição das variáveis `spring.security.oauth2.resourceserver.jwt.*` ao `application.properties`.
- [x] **Conversor de JWT:** Criação da classe `KeycloakJwtConverter` para extrair roles do Keycloak (`realm_access.roles`) e criar as respectivas `SimpleGrantedAuthority`.
- [x] **Modo Bridge:** Reescrita do `SecurityConfigurations` e `SecurityFilter`. O `SecurityFilter` agora intercepta e processa **apenas tokens do Cookie** (legacy flow). O `SecurityConfigurations` intercepta os headers `Authorization` delegando a segurança JWT OIDC.
- [x] **Configuração Exposta:** Criação do `AuthConfigController` `/api/auth/keycloak-config` para o SPA carregar as rotas do Keycloak dinamicamente.

## 2. Tarefas Futuras (Lote 2 & Lote 3) - CONCLUÍDAS
- [x] Implementar OIDC PKCE Authorization Code flow no Frontend Consorcio.
- [x] Atualizar testes E2E para adequação com OIDC.
- [x] Implementar auditoria de eventos de segurança (AuditLoggerInterceptor e SecurityAuditService assíncrono).
- [x] Aplicar restrição `@ownershipGuard` a nível de método na camada Service (AOP/IDOR prevention).
- [x] Remover infraestrutura legada (`TokenService`, `SecurityFilter`, bibliotecas `com.auth0:java-jwt`, `AutenticacaoController`).
- [x] Ativar e forçar MFA (TOTP) para administradores e compliance.
- [x] Adicionar Headers de Segurança (`CSP`, `HSTS`, `X-Frame-Options`).
