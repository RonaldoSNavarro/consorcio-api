package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.security.AuditLoggerInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuditLoggerInterceptor auditLoggerInterceptor;

    public WebConfig(AuditLoggerInterceptor auditLoggerInterceptor) {
        this.auditLoggerInterceptor = auditLoggerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Aplica auditoria a todos os endpoints da API de negócio
        registry.addInterceptor(auditLoggerInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/login/**" // exclui endpoints públicos caso ainda existam
                );
    }
}
