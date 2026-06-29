package br.com.estudo.consorcio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IntrusionDetectionFilter extends OncePerRequestFilter {

    private final IntrusionDetectionService intrusionDetectionService;

    public IntrusionDetectionFilter(IntrusionDetectionService intrusionDetectionService) {
        this.intrusionDetectionService = intrusionDetectionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        
        // Identificador pode ser IP ou Token (IP é mais global para rate limiting simples)
        // Se usar um proxy, idealmente pegar o cabeçalho X-Forwarded-For
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            clientIp = forwardedFor.split(",")[0].trim();
        }

        boolean suspicious = intrusionDetectionService.isSuspicious(clientIp);

        if (suspicious) {
            request.setAttribute("suspicious_session", true);
        }

        filterChain.doFilter(request, response);
    }
}
