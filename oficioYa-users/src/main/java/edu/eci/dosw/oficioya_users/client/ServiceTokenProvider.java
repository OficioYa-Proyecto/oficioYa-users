package edu.eci.dosw.oficioya_users.client;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Clase que genera el token JWT propio del microservicio de Users con el rol SERVICE.
 * [Lo usan WorkerClient y RequestsClient para que Worker y Servicios acepten sus llamadas.
 * Se firma con el mismo secreto que usa el microservicio de Identity]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@Component
public class ServiceTokenProvider {

    private static final long EXPIRATION_MS = 60_000;

    // sub en UUID que pide Workers
    private static final String SERVICE_SUBJECT =
            UUID.nameUUIDFromBytes("oficioya-users".getBytes(StandardCharsets.UTF_8)).toString();

    private final SecretKey key;
    private final String rolesClaim;

    /**
     * Constructor que prepara la clave de firma.
     *
     * @param secret El secreto JWT en Base64
     * @param rolesClaim El nombre del claim de roles
     */
    public ServiceTokenProvider(@Value("${jwt.secret}") String secret,
                                @Value("${jwt.roles-claim}") String rolesClaim) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.rolesClaim = rolesClaim;
    } // Cierre del constructor

    /**
     * Método que genera un token SERVICE nuevo, válido por 60 segundos, cuyo sub es un UUID fijo del
     * microservicio de Users
     *
     * @return String El encabezado "Bearer <token>", listo para enviarlo en Authorization
     */
    public String bearer() {
        long now = System.currentTimeMillis();
        return "Bearer " + Jwts.builder()
                .subject(SERVICE_SUBJECT)
                .claim(rolesClaim, List.of("SERVICE"))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    } // Cierre del método
} // Cierre de la clase