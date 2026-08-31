package br.com.estudo.consorcio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro para forçar a gravação do cookie XSRF-TOKEN em Single Page Applications (SPAs).
 * O Spring Security 6 usa carregamento deferred por padrão; ao invocar csrfToken.getToken(),
 * o token é resolvido e gravado no repositório de cookies HTTP.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Invoca getToken() para forçar o CookieCsrfTokenRepository a escrever o cookie XSRF-TOKEN
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
