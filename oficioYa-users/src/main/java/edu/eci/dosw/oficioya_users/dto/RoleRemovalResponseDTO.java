package edu.eci.dosw.oficioya_users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase DTO de una petición para quitar un rol, tal como la ven el usuario y el administrador (RF-13).
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleRemovalResponseDTO {
    private UUID id;
    private UUID userId;
    private String userName;
    private RoleName role;
    private String reason;
    private RequestStatus status;
    private String adminReason;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
} // Cierre de la clase