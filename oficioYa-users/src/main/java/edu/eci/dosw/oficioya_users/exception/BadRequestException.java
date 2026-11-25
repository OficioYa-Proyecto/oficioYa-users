package edu.eci.dosw.oficioya_users.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase de excepción para datos inválidos según las reglas de negocio
 * (error HTTP 400)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

public class BadRequestException extends ApiException {
    /**
     * Constructor que crea la excepción con código 400
     *
     * @param message El mensaje para el usuario
     */
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    } // Cierre del método
} // Cierre de la clase
