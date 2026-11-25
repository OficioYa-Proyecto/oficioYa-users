package edu.eci.dosw.oficioya_users.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase de excepción para operaciones no permitidas a esa cuenta
 * como usar una cuenta inactiva o que el admin se desactive a sí mismo
 * (error HHTP 403)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
public class ForbiddenOperationException extends ApiException {
    /**
     * Constructor que crea la excepción con código 403
     *
     * @param message El mensaje para el usuario
     */
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    } // Cierre del método
} // Cierre de la clase
