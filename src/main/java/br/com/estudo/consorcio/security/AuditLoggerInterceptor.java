package br.com.estudo.consorcio.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptador para auditoria regulatória de acesso a dados (ADR 008).
 * Registra quem acessou o quê, garantindo rastreabilidade das ações do sistema.
 */
@Component
public class AuditLoggerInterceptor implements HandlerInterceptor {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");
    
    private final br.com.estudo.consorcio.service.SecurityAuditService securityAuditService;

    public AuditLoggerInterceptor(br.com.estudo.consorcio.service.SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Extrai o usuário atual
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = "ANONYMOUS";
        String ip = request.getRemoteAddr();

        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof Jwt jwt) {
                username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getSubject();
                }
            } else {
                username = auth.getName();
            }
        }

        // Exemplo de log estruturado para sistemas de agregação (ELK, Splunk)
        auditLogger.info("AUDIT | User: {} | IP: {} | Action: {} | Resource: {}", username, ip, method, uri);

        // Salvar auditoria de forma assíncrona no banco
        securityAuditService.registrarLogAssincrono(username, ip, method, uri, "Requisição interceptada");

        return true;
    }
}
