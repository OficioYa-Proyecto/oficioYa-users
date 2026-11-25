package edu.eci.dosw.oficioya_users.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase de excepción para un recurso que no existe:
 * usuario, teléfono o petición (error HTTP 404)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

public class ResourceNotFoundException extends ApiException {
    /**
     * Constructor que crea la excepción con código 404
     *
     * @param message El mensaje para el usuario
     */
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    } // Cierre del constructor
} // Cierre de la clase
