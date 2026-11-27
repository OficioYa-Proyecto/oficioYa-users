package edu.eci.dosw.oficioya_users.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase de excepción para cuando los microservicios de Workers
 * o Services no respondan (error HTTP 503)
 * [lanzados por WorkerClient y RequestClient]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public class ServiceUnavailableException extends ApiException {
    /**
     * Constructor que crea la excepción con código 503
     *
     * @param message EL mensaje para el usuario
     */
    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    } // Cierre del constructor
} // Cierre de la clase