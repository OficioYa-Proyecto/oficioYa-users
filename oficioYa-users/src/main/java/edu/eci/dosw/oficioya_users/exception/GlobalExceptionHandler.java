package edu.eci.dosw.oficioya_users.exception;

import edu.eci.dosw.oficioya_users.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Clase que atrapa las excepciones de todos los controladores y
 * las convierte en respuestas con formato ApiError, para que el
 * front-end siempre reciba el mismo formato de error.
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Método que maneja las excepciones de negocio
     * (ApiException y sus subclases)
     *
     * @param exception La excepción lanzada
     * @param request La petición HTTP
     * @return ResponseEntity<ApiError> El error con el código que trae la excepción
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException exception, HttpServletRequest request) {
        return build(exception.getStatus(), exception.getMessage(), request);
    } // Cierre del método

    /**
     * Método que maneja los errores de @Valid y une los mensajes de cada campo
     *
     * @param exception La excepción de validación
     * @param request La petición HTTP
     * @return ResponseEntity<ApiError> Un 400 con el campo y el mensaje de cada error
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    } // Cierre del método

    /**
     * Método que maneja un cuerpo JSON mal formado o con valores que no se pueden leer
     *
     * @param exception La excepción de lectura
     * @param request La petición HTTP
     * @return ResponseEntity<ApiError> Un 400 con un mensaje genérico
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido", request);
    } // Cierre del método

    /**
     * Método que maneja una ruta que no existe.
     *
     * @param ex La excepción de recurso no encontrado
     * @param request La petición HTTP
     * @return ResponseEntity<ApiError> Un 404 en lugar de un 500
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Ruta no encontrada", request);
    } // Cierre del método

    /**
     * Método que maneja un método HTTP no permitido en una ruta que sí existe
     *
     * @param ex La excepción de método no soportado
     * @param request La petición HTTP
     * @return ResponseEntity<ApiError> Un 405 en lugar de un 500
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " no permitido en esta ruta", request);
    } // Cierre del método

    /**
     * Método que maneja un parámetro con formato inválido (UUID, enum o fecha)
     *
     * @param ex La excepción de tipo
     * @param request La petición HTTP
     * @return ResponseEntity<ApiError> Un 400 que indica el parámetro inválido
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para '" + ex.getName() + "'", request);
    } // Cierre del método

    /**
     * Método que maneja cualquier otro error y lo registra en el log
     *
     * @param exception La excepción inesperada
     * @param request La petición HTTP
     * @return ResponseEntity<ApiError> Un 500 sin exponer detalles internos
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    } // Cierre del método

    /**
     * Método que arma la respuesta de error
     *
     * @param status El código HTTP
     * @param message El mensaje para el usuario
     * @param request La petición HTTP de la que se toma la ruta
     * @return ResponseEntity<ApiError> La respuesta con el código y el cuerpo ApiError
     */
    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    } // Cierre del método
} // Cierre de la clase