package edu.eci.dosw.oficioya_users.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.eci.dosw.oficioya_users.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Clase que responde a los errores de seguridad con el mismo formato JSON
 * que usa el resto del microservicio. SecurityConfig la registra como punto
 * de entrada y como manejador de acceso denegado
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 26/11/2026
 */
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Constructor que recibe el convertidor de JSON para escribir la respuesta
     *
     * @param objectMapper El ObjectMapper que convierte el ApiError a JSON
     */
    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    } // Cierre del constructor

    /**
     * Método que se ejecuta cuando una petición llega sin autenticar a una ruta
     * protegida (sin token, con token inválido o vencido) y responde 401
     *
     * @param request La petición HTTP que fue rechazada
     * @param response La respuesta HTTP donde se escribe el error
     * @param ex La excepción de autenticación lanzada por Spring Security
     * @throws IOException Si falla la escritura de la respuesta
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "Token ausente, inválido o expirado");
    } // Cierre del método

    /**
     * Método que se ejecuta cuando un usuario autenticado intenta acceder a un
     * recurso por si su rol no alcanza y responde 403
     *
     * @param request La petición HTTP que fue rechazada
     * @param response La respuesta HTTP donde se escribe el error
     * @param ex La excepción de acceso denegado lanzada por Spring Security
     * @throws IOException Si falla la escritura de la respuesta
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "No tiene permisos para este recurso");
    } // Cierre del método

    /**
     * Método que escribe en la respuesta el estado HTTP y el cuerpo JSON con
     * el ApiError (código, motivo, mensaje y ruta de la petición)
     *
     * @param request La petición HTTP, de la que se toma la ruta
     * @param response La respuesta HTTP donde se escribe el error
     * @param status El estado HTTP que se responde (401 o 403)
     * @param mensaje El mensaje que se muestra al cliente
     * @throws IOException Si falla la escritura de la respuesta
     */
    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiError.of(status.value(), status.getReasonPhrase(), mensaje, request.getRequestURI()));
    } // Cierre del método
} // Cierre de la clase
