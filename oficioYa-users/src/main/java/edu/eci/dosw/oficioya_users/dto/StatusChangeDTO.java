package edu.eci.dosw.oficioya_users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Clase DTO con el motivo (10 a 500 caracteres) que exige el admin
 * al activar o desactivar una cuenta y al aprobar
 * o rechazar una solicitud de quitar rol
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@Data
public class StatusChangeDTO {
    @NotBlank(message = "El motivo es obligatorio")
    @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
    private String reason;
} // Cierre de la clase
