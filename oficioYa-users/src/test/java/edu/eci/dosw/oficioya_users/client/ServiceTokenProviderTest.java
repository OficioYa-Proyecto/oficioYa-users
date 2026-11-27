package edu.eci.dosw.oficioya_users.client;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Clase de pruebas de ServiceTokenProvider
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
class ServiceTokenProviderTest {

    private static final String SECRET = "IadhiSp/AVyloXuE15l29oZzpucys4NEfqpHn6PEMeQ=";

    /**
     * Prueba que el token está firmado con el secreto y trae el rol SERVICE en el claim de roles configurado
     */
    @Test
    void bearerIsSignedServiceTokenWithConfiguredRolesClaim() {
        String bearer = new ServiceTokenProvider(SECRET, "roles").bearer();

        assertThat(bearer).startsWith("Bearer ");
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(bearer.substring("Bearer ".length()))
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo(java.util.UUID.nameUUIDFromBytes("oficioya-users".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        assertThat(claims.get("roles", List.class)).containsExactly("SERVICE");
        assertThat(claims.getExpiration()).isInTheFuture();
    } // Cierre del método
} // Cierre de la clase