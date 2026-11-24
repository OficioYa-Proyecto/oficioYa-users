package edu.eci.dosw.oficioya_users.controller;

import edu.eci.dosw.oficioya_users.dto.RoleRemovalRequestDTO;
import edu.eci.dosw.oficioya_users.dto.RoleRemovalResponseDTO;
import edu.eci.dosw.oficioya_users.dto.StatusChangeDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.service.RoleRemovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Quitar rol", description = "RF-13. El usuario pide quitarse un rol y el administrador lo resuelve")
public class RoleRemovalController {

    private final RoleRemovalService roleRemovalService;

    @PostMapping("/users/me/role-removal-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pedir quitar un rol", description = "RF-13. Razón de 10 a 500 caracteres. 400 si es ADMIN, "
            + "403 si la cuenta está inactiva o es de admin, 409 si no tiene el rol, es el único o ya tiene una petición pendiente")
    public RoleRemovalResponseDTO create(@AuthenticationPrincipal UUID userId,
                                         @Valid @RequestBody RoleRemovalRequestDTO request) {
        return roleRemovalService.create(userId, request);
    }

    @GetMapping("/users/me/role-removal-requests")
    @Operation(summary = "Consultar mis peticiones", description = "RF-13. Las más recientes primero")
    public List<RoleRemovalResponseDTO> findOwn(@AuthenticationPrincipal UUID userId) {
        return roleRemovalService.findOwn(userId);
    }

    @GetMapping("/admin/role-removal-requests")
    @Operation(summary = "Listar peticiones", description = "RF-13. Filtros opcionales status (PENDING, APPROVED, REJECTED) y userId")
    public List<RoleRemovalResponseDTO> findAll(@RequestParam(required = false) RequestStatus status,
                                                @RequestParam(required = false) UUID userId) {
        return roleRemovalService.findAll(status, userId);
    }

    @PatchMapping("/admin/role-removal-requests/{id}/approve")
    @Operation(summary = "Aprobar petición", description = "RF-13. Quita el rol con las mismas reglas que DELETE /admin/users/{id}/roles/{role}; "
            + "requiere motivo. 404 si no existe, 409 si ya fue resuelta o la cuenta tiene solicitudes activas en ese rol, "
            + "503 si Servicios o Worker no responden")
    public RoleRemovalResponseDTO approve(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                          @Valid @RequestBody StatusChangeDTO request) {
        return roleRemovalService.approve(adminId, id, request.getReason());
    }

    @PatchMapping("/admin/role-removal-requests/{id}/reject")
    @Operation(summary = "Rechazar petición", description = "RF-13. Requiere motivo. 409 si ya fue resuelta")
    public RoleRemovalResponseDTO reject(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                         @Valid @RequestBody StatusChangeDTO request) {
        return roleRemovalService.reject(adminId, id, request.getReason());
    }

    @DeleteMapping("/admin/users/{id}/roles/{role}")
    @Operation(summary = "Quitar rol (admin)", description = "RF-13. Quita TRABAJADOR o CONTRATANTE de una cuenta, activa o inactiva, "
            + "sin body. Si había una petición pendiente para ese rol queda aprobada. Al quitar TRABAJADOR pausa su perfil en Worker "
            + "y la localidad se conserva. 400 si piden ADMIN, 403 si quien llama no es admin o la cuenta es de admin, "
            + "404 si la cuenta no existe, 409 si no tiene el rol, es el único o tiene solicitudes activas en ese rol, "
            + "503 si Servicios o Worker no responden")
    public UserResponseDTO removeRole(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                      @PathVariable RoleName role) {
        return roleRemovalService.removeByAdmin(adminId, id, role);
    }
}