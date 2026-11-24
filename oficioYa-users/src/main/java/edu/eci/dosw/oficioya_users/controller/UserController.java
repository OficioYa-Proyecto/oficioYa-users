package edu.eci.dosw.oficioya_users.controller;

import edu.eci.dosw.oficioya_users.dto.AccountStatsDTO;
import edu.eci.dosw.oficioya_users.dto.PhoneDTO;
import edu.eci.dosw.oficioya_users.dto.UserRequestDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.dto.UserUpdateDTO;
import edu.eci.dosw.oficioya_users.dto.StatusChangeDTO;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Registro, perfil propio, perfiles públicos y administración de cuentas")
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar cuenta", description = "RF-01. Crea la cuenta activa con rol TRABAJADOR y/o CONTRATANTE. "
            + "birthDate (AAAA-MM-DD) es obligatoria; 400 si falta, es menor de 18 años, futura o anterior a 1900. localityId (localidad, RF-14) es opcional; 400 si no existe")
    public UserResponseDTO register(@Valid @RequestBody UserRequestDTO request) {
        return userService.register(request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar perfil", description = "Compensación del registro [Orquestador]")
    public void delete(@PathVariable UUID id) {
        userService.deleteById(id);
    }

    @GetMapping("/users")
    @Operation(summary = "Consultar varios usuarios", description = "RF-10. Datos públicos de las cuentas activas (máximo 50 ids)")
    public List<UserResponseDTO> findByIds(@RequestParam(defaultValue = "") List<UUID> ids) {
        return userService.findPublicByIds(ids);
    }

    @GetMapping("/users/me")
    @Operation(summary = "Consultar perfil propio", description = "RF-03. Correo y teléfono enmascarados")
    public UserResponseDTO getOwnProfile(@AuthenticationPrincipal UUID userId) {
        return userService.getOwnProfile(userId);
    }

    @PatchMapping("/users/me")
    @Operation(summary = "Editar perfil propio", description = "RF-04. Nombre, teléfonos y localidad (localityId, RF-14). "
            + "El trabajador y el contratante pueden cambiar su localidad; 403 si la cuenta es de admin")
    public UserResponseDTO updateOwnProfile(@AuthenticationPrincipal UUID userId,
                                            @Valid @RequestBody UserUpdateDTO request) {
        return userService.updateOwnProfile(userId, request);
    }

    @PostMapping("/users/me/roles/{role}")
    @Operation(summary = "Agregar rol", description = "RF-02. Agrega el rol faltante a la misma cuenta. "
            + "Si es TRABAJADOR y ya tenía perfil en Worker, lo reactiva (ACTIVE)")
    public UserResponseDTO addRole(@AuthenticationPrincipal UUID userId, @PathVariable RoleName role) {
        return userService.addRole(userId, role);
    }

    @GetMapping("/users/me/phones")
    @Operation(summary = "Consultar teléfonos propios", description = "RF-04. Números completos para editarlos")
    public List<PhoneDTO> getOwnPhones(@AuthenticationPrincipal UUID userId) {
        return userService.getOwnPhones(userId);
    }

    @PostMapping("/users/me/phones")
    @Operation(summary = "Agregar teléfono", description = "RF-04. Agrega un teléfono adicional (máximo 3 en total)")
    public List<PhoneDTO> addPhone(@AuthenticationPrincipal UUID userId, @Valid @RequestBody PhoneDTO request) {
        return userService.addPhone(userId, request);
    }

    @PatchMapping("/users/me/phones/{phoneId}")
    @Operation(summary = "Cambiar número", description = "RF-04. Cambia el número de un teléfono propio")
    public List<PhoneDTO> updatePhone(@AuthenticationPrincipal UUID userId, @PathVariable UUID phoneId,
                                       @Valid @RequestBody PhoneDTO request) {
        return userService.updatePhone(userId, phoneId, request);
    }

    @DeleteMapping("/users/me/phones/{phoneId}")
    @Operation(summary = "Eliminar teléfono", description = "RF-04. El teléfono principal no se puede eliminar")
    public List<PhoneDTO> removePhone(@AuthenticationPrincipal UUID userId, @PathVariable UUID phoneId) {
        return userService.removePhone(userId, phoneId);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Consultar perfil público", description = "RF-05. Sin correo ni teléfono")
    public UserResponseDTO getPublicProfile(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return userService.getPublicProfile(userId, id);
    }

    @GetMapping("/admin/users")
    @Operation(summary = "Listar usuarios", description = "Datos completos de todas las cuentas")
    public List<UserResponseDTO> findAll() {
        return userService.findAll();
    }

    @GetMapping("/admin/users/{id}")
    @Operation(summary = "Consultar usuario", description = "RF-08. Datos completos de una cuenta")
    public UserResponseDTO findById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PatchMapping("/admin/users/{id}/deactivate")
    @Operation(summary = "Desactivar cuenta", description = "RF-06. Requiere motivo")
    public UserResponseDTO deactivate(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                      @Valid @RequestBody StatusChangeDTO request) {
        return userService.changeStatus(adminId, id, AccountStatus.INACTIVE, request.getReason());
    }

    @PatchMapping("/admin/users/{id}/activate")
    @Operation(summary = "Reactivar cuenta", description = "RF-07. Requiere motivo")
    public UserResponseDTO activate(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                    @Valid @RequestBody StatusChangeDTO request) {
        return userService.changeStatus(adminId, id, AccountStatus.ACTIVE, request.getReason());
    }

    @GetMapping("/users/stats/accounts")
    @Operation(summary = "Estadísticas de cuentas",
            description = "RF-12. Solo ADMIN (Reporting reenvía el token del administrador). Cuentas registradas hasta 'to' "
                    + "y desactivadas entre 'from' y 'to', en total y por rol. Fechas en formato AAAA-MM-DD")
    public AccountStatsDTO accountStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return userService.getAccountStats(from, to);
    }

    @GetMapping("/users/by-email")
    @Operation(summary = "Consultar cuenta por correo", description = "RF-11. Solo el rol SERVICE")
    public UserResponseDTO findByEmail(@RequestParam(defaultValue = "") String email) {
        return userService.findByEmail(email);
    }

    @GetMapping("/users/{id}/account")
    @Operation(summary = "Consultar cuenta por id", description = "RF-11. Solo el rol SERVICE")
    public UserResponseDTO findAccount(@PathVariable UUID id) {
        return userService.findById(id);
    }
}