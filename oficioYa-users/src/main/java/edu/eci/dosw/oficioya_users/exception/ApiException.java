package edu.eci.dosw.oficioya_users.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase abstracta base de las excepciones de negocio.
 * Guarda el código HTTP que debe responder
 * GlobalExceptionHandler; cada subclase fija el suyo
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;

    /**
     * Constructor que guarda el código HTTP y el mensaje
     *
     * @param status El código HTTP de la respuesta
     * @param message El mensaje para el usuario
     */
    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    } // Cierre del método

    /**
     * Método que obtiene el código HTTP
     *
     * @return HttpStatus El código para que GlobalExceptionHandler arme la respuesta
     */
    public HttpStatus getStatus() {
        return status;
    } // Cierre del método
} // Cierre de la clase
