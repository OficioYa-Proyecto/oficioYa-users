package edu.eci.dosw.oficioya_users.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Clase de configuración para la API sin sesión (Stateless),
 * autenticación con JWT y permisos por ruta y rol.
 * Usa Spring Security
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

@Configuration
public class SecurityConfig {

    /**
     * Método que define qué rutas son públicas, cuáles son solo para SERVICE,
     * cuáles solo para ADMIN y que el resto requiera token
     *
     * @param http La configuración HTTP de Spring Security
     * @param objectMapper El convertidor JSON para errores
     * @param secret El secreto JWT en Base64
     * @param rolesClaim El nombre del claim de roles
     * @return SecurityFilterChain La cadena de seguridad aplicada
     * a cada petición
     * @throws Exception Las excepciones (errores) que retorna Spring Security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper objectMapper,
                                                   @Value("${jwt.secret}") String secret,
                                                   @Value("${jwt.roles-claim}") String rolesClaim) throws Exception {
        JsonSecurityErrorHandler errorHandler = new JsonSecurityErrorHandler(objectMapper);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/by-email").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}/account").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/stats/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/zones").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/localities").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .addFilterBefore(new JwtAuthenticationFilter(secret, rolesClaim), UsernamePasswordAuthenticationFilter.class)
                .build();
    } // Cierre del método
} // Cierre de la clase