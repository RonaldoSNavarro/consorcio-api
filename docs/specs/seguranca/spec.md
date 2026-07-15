# Especificação Lote 1 — Bridge OAuth2 e Keycloak Docker

## 1. Visão Geral
Este documento descreve a especificação técnica da migração (Lote 1) da camada de autenticação do `consorcio-api`, substituindo gradualmente a autenticação legada (JWT HMAC256 via cookie) por OAuth2/OIDC Resource Server (JWT RS256) hospedado no Keycloak.

## 2. Objetivos
- Subir instância do Keycloak via Docker Compose na porta 8180.
- Configurar Realm (`consorcio`), Clients (`consorcio-api` e `consorcio-frontend`) e usuários de teste.
- Integrar `spring-boot-starter-oauth2-resource-server` na API Spring Boot.
- Implementar modo **Bridge**, onde a API aceita tanto os novos tokens RS256 (via Header `Authorization: Bearer`) quanto os tokens antigos HMAC256 (via Cookie `token`).

## 3. Decisões Arquiteturais (ADR 008)
- **IdP (Identity Provider):** Keycloak 26.2
- **Segurança de Criptografia:** RS256 com validação assimétrica via JWKS URI.
- **Autorização (RBAC):** Os claims `realm_access.roles` do token Keycloak são convertidos nativamente em `GrantedAuthority` do Spring Security (prefixo `ROLE_`), mantendo retrocompatibilidade com anotações `@PreAuthorize` e `.hasAnyRole()`.

## 4. Modificações de Infraestrutura
- O `docker-compose.yml` mapeia a API para a porta `8081:8081` (evitando conflito com a porta `8080` do host).
- O Keycloak usa a porta `8180:8080` no host.
- A inicialização do Keycloak importa automaticamente o realm configurado no JSON exportado (`keycloak/realm-consorcio.json`).

## 5. Status dos Lotes
1. **Lote 1:** Concluído (Bridge OAuth2, Configuração Docker do Keycloak, Converter JWT).
2. **Lote 2:** Concluído (Remoção da infraestrutura legada de autenticação `AutenticacaoController`, `TokenService`, `SecurityFilter`, e ajuste nos testes). Configuração do Frontend para integração nativa.
3. **Lote 3 (Hardening):** Concluído (MFA por E-mail exigido para `COMPLIANCE` e `ADMIN` com e-mail cadastrado como `ronaldoguitarrista@gmail.com`, Refatoração ABAC para `@PreAuthorize` na camada de Service, IDOR Guard, Auditoria Assíncrona salva em banco via Flyway `security_audit_log`, Security Headers, Migração do MFA TOTP para Código enviado por E-mail).
