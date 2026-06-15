package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("API Consorcio")
                    .withSubject(usuario.getUsername())
                    .withClaim("id", usuario.getId())
                    .withClaim("role", usuario.getRole())
                    .withClaim("nome", usuario.getNome())
                    .withClaim("email", usuario.getEmail())
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException exception) {
            throw new RegraDeNegocioException("Erro ao gerar token JWT");
        }
    }

    private Instant dataExpiracao() {
        // Define o fuso horário de Brasília (-03:00) e adiciona 2 horas de validade
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    public com.auth0.jwt.interfaces.DecodedJWT verificarToken(String tokenJWT) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.require(algoritmo)
                    .withIssuer("API Consorcio")
                    .build()
                    .verify(tokenJWT);
        } catch (Exception exception) {
            throw new RegraDeNegocioException("Token JWT inválido ou expirado!");
        }
    }

    public String getSubject(String tokenJWT) {
        return verificarToken(tokenJWT).getSubject();
    }
}