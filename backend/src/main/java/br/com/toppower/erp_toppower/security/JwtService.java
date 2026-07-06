package br.com.toppower.erp_toppower.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    /**
     * Tamanho mínimo do segredo, em bytes, exigido pelo algoritmo HS256 (256 bits).
     */
    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-hours:24}") long expirationHours) {
        validateSecret(secret);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(expirationHours);
    }

    /**
     * Falha cedo (no boot) caso o secret configurado seja curto demais para HS256,
     * evitando um {@link InvalidKeyException} genérico apenas na primeira geração de token.
     */
    private static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "A propriedade 'jwt.secret' não foi configurada. Defina JWT_SECRET no .env.");
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(String.format(
                    "A propriedade 'jwt.secret' deve ter ao menos %d bytes (%d bits) para HS256, "
                            + "mas possui %d bytes. Gere um novo secret com: openssl rand -base64 48",
                    MIN_SECRET_BYTES, MIN_SECRET_BYTES * 8, length));
        }
    }

/**
 * Gera um JWT assinado (HS256) contendo subject (e-mail), role e o tenant
 * da sessão (UUID da empresa selecionada no login). O claim {@code tenant}
 * é lido pelo {@code JwtAuthenticationFilter} para popular o
 * {@code TenantContext}, que habilita o filtro de isolamento por tenant.
 */
public String generateToken(UserDetailsImpl user) {
    Instant now = Instant.now();
    Instant exp = now.plus(expiration);
    var builder = Jwts.builder()
            .subject(user.email())
            .claim("role", user.role());
    if (user.tenantUuid() != null) {
        builder.claim("tenant", user.tenantUuid().toString());
    }
    return builder
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(signingKey)
            .compact();
}

    /**
     * Extrai o subject (e-mail) do token, validando assinatura e expiração.
     * Retorna vazio se o token for inválido ou estiver expirado.
     */
    public Optional<String> extractEmail(String token) {
        try {
            Claims claims = parseClaims(token);
            return Optional.of(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Extrai o claim {@code tenant} (UUID do tenant da sessão) do token.
     * Retorna vazio se o token for inválido/expirado ou se não houver
     * claim de tenant (tokens legados, pré-multi-tenancy).
     */
    public Optional<UUID> extractTenant(String token) {
        try {
            Claims claims = parseClaims(token);
            String tenant = claims.get("tenant", String.class);
            if (tenant == null || tenant.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(tenant));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }
}
