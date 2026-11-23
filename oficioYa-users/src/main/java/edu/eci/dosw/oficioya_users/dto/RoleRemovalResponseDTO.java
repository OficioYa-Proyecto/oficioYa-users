package edu.eci.dosw.oficioya_users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** RF-13. Petición para quitar un rol, tal como la ven el usuario y el administrador */
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
}