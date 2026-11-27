package edu.eci.dosw.oficioya_users.dto;

import edu.eci.dosw.oficioya_users.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Clase DTO con lo que envía el usuario para pedir que le quiten un rol (RF-13):
 * el rol y la razón, de 10 a 500 caracteres
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@Data
public class RoleRemovalRequestDTO {
    @NotNull(message = "El rol es obligatorio")
    private RoleName role;

    @NotBlank(message = "La razón es obligatoria")
    @Size(min = 10, max = 500, message = "La razón debe tener entre 10 y 500 caracteres")
    private String reason;
} // Cierre de la clase