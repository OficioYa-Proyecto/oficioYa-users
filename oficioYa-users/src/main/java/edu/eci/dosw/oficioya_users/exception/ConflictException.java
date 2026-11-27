package edu.eci.dosw.oficioya_users.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase de excepción para operaciones que chocan con el estado actual
 * como un correo repetido o un rol que ya se tiene (error HTTP 409)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public class ConflictException extends ApiException{
    /**
     * Constructor que crea la excepción con código 409
     *
     * @param message El mensaje para el usuario
     */
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    } // Cierre del método
} // Cierre de la clase
