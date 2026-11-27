package edu.eci.dosw.oficioya_users.dto;

import java.time.LocalDateTime;

/**
 * Record con el formato único de los errores del microservicio.
 * [usado por GlobalExceptionHandler y JsonSecurityErrorHandler
 * para que el front reciba siempre la misma estructura]
 *
 * @param timestamp La fecha y hora del error
 * @param status El código HTTP
 * @param error El nombre del código HTTP
 * @param message El mensaje para el usuario
 * @param route La ruta que se llamó
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String route) {

    /**
     * Método que crea un error con la fecha y hora actuales
     *
     * @param status El código HTTP
     * @param error El nombre del código HTTP
     * @param message El mensaje para el usuario
     * @param route La ruta que se llamó
     * @return ApiError El error listo para enviarlo en la respuesta
     */
    public static ApiError of(int status, String error, String message, String route) {
        return new ApiError(LocalDateTime.now(), status, error, message, route);
    } // Cierre del método
} // Cierre del record
