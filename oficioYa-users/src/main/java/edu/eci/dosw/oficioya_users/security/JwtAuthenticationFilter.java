package edu.eci.dosw.oficioya_users.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Clase de filtro que se ejecuta una vez por solicitud y valida
 * el token JWT del microservicio de Identity (u otro token SERVICE
 * de otro microservicio).
 * Si es válido,, deja autenticado al usuario con su id (sub) y sus roles,
 * sino entonces la solicitud sigue sin autenticar y SecurityConfig responde
 * 401 con las rutas protegidas
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey key;
    private final String rolesClaim;

    /**
     * Constructor que prepara la clave para verificar la firma HS256
     *
     * @param secret El secreto JWT en Base64
     * @param rolesClaim El nombre del claim de roles
     */
    public JwtAuthenticationFilter(String secret, String rolesClaim) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.rolesClaim = rolesClaim;
    } // Cierre del constructor

    /**
     * Método que lee el encabezado Authorization, verifica el token y
     * guarda la autenticación en el contexto de seguridad. Añade
     * ROLE_ a cada rol si no lo tiene para usar el método hasRole
     *
     * @param request La petición HTTP
     * @param response La respuesta HTTP
     * @param chain La cadena de filtros para continuar con la petición
     * @throws ServletException Indica que hubo un problema al procesar una petición
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(header.substring(7)).getPayload();
                UUID userId = UUID.fromString(claims.getSubject());
                List<?> roles = claims.get(rolesClaim, List.class);
                List<SimpleGrantedAuthority> authorities = roles == null ? List.of() : roles.stream()
                        .map(String::valueOf)
                        .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                        .toList();
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, authorities));
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    } // Cierre del método
} // Cierre de la clase
