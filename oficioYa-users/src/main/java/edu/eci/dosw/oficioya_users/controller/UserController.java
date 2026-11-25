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

/**
 * Clase controladora que expone los endpoints de cuentas: registro, perfil propio y público, teléfonos,
 * roles, administración de cuentas, estadísticas para el microservicio de Reporting y
 * consultas internas para el microservicio de Identity.
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Registro, perfil propio, perfiles públicos y administración de cuentas")
public class UserController {

    private final UserService userService;

    /**
     * Método que atiende el endpoint POST /api/v1/users (RF-01).
     * Solo lo llama el Orquestador con rol SERVICE
     *
     * @param request Los datos del registro
     * @return UserResponseDTO La cuenta creada (201), para que el Orquestador continúe el registro en
     * el microservicio de Identity
     */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar cuenta", description = "RF-01. Crea la cuenta activa con rol TRABAJADOR y/o CONTRATANTE. "
            + "birthDate (AAAA-MM-DD) es obligatoria; 400 si falta, es menor de 18 años, futura o anterior a 1900. localityId (localidad, RF-14) es obligatoria; 400 si falta o no existe")
    public UserResponseDTO register(@Valid @RequestBody UserRequestDTO request) {
        return userService.register(request);
    } // Cierre del método

    /**
     * Método que atiende el endpoint DELETE /api/v1/users/{id}
     *
     * @param id El id del perfil a eliminar
     */
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar perfil", description = "Compensación del registro [Orquestador]")
    public void delete(@PathVariable UUID id) {
        userService.deleteById(id);
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/users?ids= (RF-10)
     *
     * @param ids Los ids de las cuentas (máximo 50)
     * @return List<UserResponseDTO> Los datos públicos de las cuentas activas, para que otros microservicios
     * consulten varios usuarios en una sola petición
     */
    @GetMapping("/users")
    @Operation(summary = "Consultar varios usuarios", description = "RF-10. Datos públicos de las cuentas activas (máximo 50 ids)")
    public List<UserResponseDTO> findByIds(@RequestParam(defaultValue = "") List<UUID> ids) {
        return userService.findPublicByIds(ids);
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/users/me (RF-03)
     *
     * @param userId El id del usuario autenticado, tomado del sub del token
     * @return UserResponseDTO El perfil propio con correo y teléfonos enmascarados
     */
    @GetMapping("/users/me")
    @Operation(summary = "Consultar perfil propio", description = "RF-03. Correo y teléfono enmascarados")
    public UserResponseDTO getOwnProfile(@AuthenticationPrincipal UUID userId) {
        return userService.getOwnProfile(userId);
    } // Cierre del método

    /**
     * Método que atiende el endpoint PATCH /api/v1/users/me (RF-04)
     *
     * @param userId El id del usuario autenticado, tomado del sub del token
     * @param request Los campos a modificar (nombre, teléfonos, localidad)
     * @return UserResponseDTO El perfil actualizado
     */
    @PatchMapping("/users/me")
    @Operation(summary = "Editar perfil propio", description = "RF-04. Nombre, teléfonos y localidad (localityId, RF-14). "
            + "El trabajador y el contratante pueden cambiar su localidad; 403 si la cuenta es de admin")
    public UserResponseDTO updateOwnProfile(@AuthenticationPrincipal UUID userId,
                                            @Valid @RequestBody UserUpdateDTO request) {
        return userService.updateOwnProfile(userId, request);
    } // Cierre del método

    /**
     * Método que atiende el endpoint POST /api/v1/users/me/roles/{role} (RF-02)
     *
     * @param userId El id del usuario autenticado, tomado del sub del token
     * @param role El rol que se quiere agregar
     * @return UserResponseDTO El perfil con el nuevo rol
     */
    @PostMapping("/users/me/roles/{role}")
    @Operation(summary = "Agregar rol", description = "RF-02. Agrega el rol faltante a la misma cuenta. "
            + "Si es TRABAJADOR y ya tenía perfil en Worker, lo reactiva (ACTIVE)")
    public UserResponseDTO addRole(@AuthenticationPrincipal UUID userId, @PathVariable RoleName role) {
        return userService.addRole(userId, role);
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/users/me/phones (RF-04)
     *
     * @param userId El id del usuario autenticado, tomado del sub del token
     * @return List<PhoneDTO> Los teléfonos completos con su id, para poder editarlos
     */
    @GetMapping("/users/me/phones")
    @Operation(summary = "Consultar teléfonos propios", description = "RF-04. Números completos para editarlos")
    public List<PhoneDTO> getOwnPhones(@AuthenticationPrincipal UUID userId) {
        return userService.getOwnPhones(userId);
    } // Cierre del método

    /**
     * Método que atiende el endpoint POST /api/v1/users/me/phones (RF-04)
     *
     * @param userId El id del usuario autenticado, tomado del sub del token
     * @param request El teléfono adicional
     * @return List<PhoneDTO> Los teléfonos actualizados
     */
    @PostMapping("/users/me/phones")
    @Operation(summary = "Agregar teléfono", description = "RF-04. Agrega un teléfono adicional (máximo 3 en total)")
    public List<PhoneDTO> addPhone(@AuthenticationPrincipal UUID userId, @Valid @RequestBody PhoneDTO request) {
        return userService.addPhone(userId, request);
    } // Cierre del método

    /**
     * Método que atiende el endpoint PATCH /api/v1/users/me/phones/{phoneId} (RF-04)
     *
     * @param userId El id del usuario autenticado, tomado del sub del token
     * @param phoneId El id del teléfono a cambiar
     * @param request El teléfono con el número nuevo
     * @return List<PhoneDTO> Los teléfonos actualizados
     */
    @PatchMapping("/users/me/phones/{phoneId}")
    @Operation(summary = "Cambiar número", description = "RF-04. Cambia el número de un teléfono propio")
    public List<PhoneDTO> updatePhone(@AuthenticationPrincipal UUID userId, @PathVariable UUID phoneId,
                                       @Valid @RequestBody PhoneDTO request) {
        return userService.updatePhone(userId, phoneId, request);
    } // Cierre del método

    /**
     * Método que atiende el endpoint DELETE /api/v1/users/me/phones/{phoneId} (RF-04)
     *
     * @param userId El id del usuario autenticado, tomado del sub del token
     * @param phoneId El id del teléfono a eliminar (no puede ser el principal)
     * @return List<PhoneDTO> Los teléfonos que quedan
     */
    @DeleteMapping("/users/me/phones/{phoneId}")
    @Operation(summary = "Eliminar teléfono", description = "RF-04. El teléfono principal no se puede eliminar")
    public List<PhoneDTO> removePhone(@AuthenticationPrincipal UUID userId, @PathVariable UUID phoneId) {
        return userService.removePhone(userId, phoneId);
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/users/{id} (RF-05)
     *
     * @param userId El id del usuario autenticado que consulta
     * @param id El id de la cuenta que se quiere ver
     * @return UserResponseDTO El perfil público, sin correo ni teléfonos
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Consultar perfil público", description = "RF-05. Sin correo ni teléfono")
    public UserResponseDTO getPublicProfile(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return userService.getPublicProfile(userId, id);
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/admin/users [solo para ADMIN]
     *
     * @return List<UserResponseDTO> Todas las cuentas con datos completos para el listado del admin
     */
    @GetMapping("/admin/users")
    @Operation(summary = "Listar usuarios", description = "Datos completos de todas las cuentas")
    public List<UserResponseDTO> findAll() {
        return userService.findAll();
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/admin/users/{id} (RF-08) [solo para ADMIN]
     *
     * @param id El id de la cuenta
     * @return UserResponseDTO La cuenta con datos completos para el detalle del admin
     */
    @GetMapping("/admin/users/{id}")
    @Operation(summary = "Consultar usuario", description = "RF-08. Datos completos de una cuenta")
    public UserResponseDTO findById(@PathVariable UUID id) {
        return userService.findById(id);
    } // Cierre del método

    /**
     * Método que atiende el endpoint PATCH /api/v1/admin/users/{id}/deactivate (RF-06)
     *
     * @param adminId El id del administrador autenticado, tomado del sub del token
     * @param id El id de la cuenta
     * @param request El motivo de la desactivación
     * @return UserResponseDTO La cuenta inactiva
     */
    @PatchMapping("/admin/users/{id}/deactivate")
    @Operation(summary = "Desactivar cuenta", description = "RF-06. Requiere motivo")
    public UserResponseDTO deactivate(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                      @Valid @RequestBody StatusChangeDTO request) {
        return userService.changeStatus(adminId, id, AccountStatus.INACTIVE, request.getReason());
    } // Cierre del método

    /**
     * Método que atiende el endpoint PATCH /api/v1/admin/users/{id}/activate (RF-07)
     *
     * @param adminId El id del administrador autenticado, tomado del sub del token
     * @param id El id de la cuenta
     * @param request El motivo de la reactivación
     * @return UserResponseDTO La cuenta activa
     */
    @PatchMapping("/admin/users/{id}/activate")
    @Operation(summary = "Reactivar cuenta", description = "RF-07. Requiere motivo")
    public UserResponseDTO activate(@AuthenticationPrincipal UUID adminId, @PathVariable UUID id,
                                    @Valid @RequestBody StatusChangeDTO request) {
        return userService.changeStatus(adminId, id, AccountStatus.ACTIVE, request.getReason());
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/users/stats/accounts (RF-12) [solo para ADMIN]
     *
     * @param from La fecha de inicio del periodo (AAAA-MM-DD)
     * @param to La fecha de fin del periodo (AAAA-MM-DD)
     * @return AccountStatsDTO Las cuentas registradas y desactivadas por rol para el reporte de cuentas
     * eliminadas
     */
    @GetMapping("/users/stats/accounts")
    @Operation(summary = "Estadísticas de cuentas",
            description = "RF-12. Solo ADMIN (Reporting reenvía el token del administrador). Cuentas registradas hasta 'to' "
                    + "y desactivadas entre 'from' y 'to', en total y por rol. Fechas en formato AAAA-MM-DD")
    public AccountStatsDTO accountStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return userService.getAccountStats(from, to);
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/users/by-email (RF-11) [solo para el rol SERVICE]
     *
     * @param email El correo de la cuenta
     * @return UserResponseDTO La cuenta con su estado para que
     * el microservicio de Identity valide el inicio de sesión
     */
    @GetMapping("/users/by-email")
    @Operation(summary = "Consultar cuenta por correo", description = "RF-11. Solo el rol SERVICE")
    public UserResponseDTO findByEmail(@RequestParam(defaultValue = "") String email) {
        return userService.findByEmail(email);
    } // Cierre del método

    /**
     * Método que atiende el endpoint GET /api/v1/users/{id}/account (RF-11) [solo para el rol SERVICE]
     *
     * @param id El id de la cuenta
     * @return UserResponseDTO La cuenta con su estado y roles para que Identity genere el token
     */
    @GetMapping("/users/{id}/account")
    @Operation(summary = "Consultar cuenta por id", description = "RF-11. Solo el rol SERVICE")
    public UserResponseDTO findAccount(@PathVariable UUID id) {
        return userService.findById(id);
    } // Cierre del método
} // Cierre de la clase