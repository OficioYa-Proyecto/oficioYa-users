package edu.eci.dosw.oficioya_users.dto;

import edu.eci.dosw.oficioya_users.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** RF-13. Lo que envía el usuario para pedir que le quiten un rol */
@Data
public class RoleRemovalRequestDTO {
    @NotNull(message = "El rol es obligatorio")
    private RoleName role;

    @NotBlank(message = "La razón es obligatoria")
    @Size(min = 10, max = 500, message = "La razón debe tener entre 10 y 500 caracteres")
    private String reason;
}