package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.client.RequestsClient;
import edu.eci.dosw.oficioya_users.client.RequestsClient.RoleRemovalNotification;
import edu.eci.dosw.oficioya_users.dto.RoleRemovalRequestDTO;
import edu.eci.dosw.oficioya_users.dto.RoleRemovalResponseDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.entity.RoleRemovalRequestEntity;
import edu.eci.dosw.oficioya_users.entity.UserEntity;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.exception.BadRequestException;
import edu.eci.dosw.oficioya_users.exception.ConflictException;
import edu.eci.dosw.oficioya_users.exception.ForbiddenOperationException;
import edu.eci.dosw.oficioya_users.exception.ResourceNotFoundException;
import edu.eci.dosw.oficioya_users.repository.RoleRemovalRequestRepository;
import edu.eci.dosw.oficioya_users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Clase que permite al usuario pedir que le quiten un rol con una razón
 * y solo el administrador lo quita
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@Slf4j
@Service
@Transactional
public class RoleRemovalService {

    static final String DIRECT_REMOVAL_REASON = "El administrador quitó el rol desde el detalle de la cuenta";
    static final String ACTION_REQUESTED = "REQUESTED";
    static final String ACTION_APPROVED = "APPROVED";
    static final String ACTION_REJECTED = "REJECTED";

    private final RoleRemovalRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RequestsClient requestsClient;
    private final String adminEmail;

    /**
     * Constructor que usa Spring con las dependencias del servicio.
     *
     * @param requestRepository El repositorio de las peticiones de quitar rol.
     * @param userRepository El repositorio de usuarios.
     * @param userService El servicio de usuarios, que aplica las reglas para quitar el rol.
     * @param requestsClient El cliente de Servicios, que envía los correos.
     * @param adminNotificationEmail El correo del administrador, destinatario de los avisos de
     *                   peticiones nuevas.
     */
    public RoleRemovalService(RoleRemovalRequestRepository requestRepository, UserRepository userRepository,
                              UserService userService, RequestsClient requestsClient,
                              @Value("${admin.notification-email:}") String adminNotificationEmail) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.requestsClient = requestsClient;
        this.adminEmail = adminNotificationEmail;
    } // Cierre del método

    /**
     * Método que crea la petición. No se puede pedir quitar ADMIN, la cuenta debe estar activa, no ser de
     * admin, tener el rol, conservar al menos otro rol y no tener otra petición pendiente.
     *
     * @param userId El id del usuario autenticado, tomado del sub del token.
     * @param request El rol y la razón.
     * @return RoleRemovalResponseDTO La petición guardada en PENDING, para mostrarla al usuario.
     */
    public RoleRemovalResponseDTO create(UUID userId, RoleRemovalRequestDTO request) {
        RoleName role = request.getRole();
        if (role == RoleName.ADMIN) {
            throw new BadRequestException("El rol ADMIN no se puede quitar");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenOperationException("La cuenta está inactiva");
        }
        if (hasRole(user, RoleName.ADMIN)) {
            throw new ForbiddenOperationException("El administrador no puede quitarse roles");
        }
        if (!hasRole(user, role)) {
            throw new ConflictException("La cuenta no tiene el rol " + role);
        }
        if (user.getRoles().size() == 1) {
            throw new ConflictException("La cuenta debe conservar al menos un rol");
        }
        if (requestRepository.existsByUserIdAndStatus(userId, RequestStatus.PENDING)) {
            throw new ConflictException("Ya tienes una petición pendiente; espera a que el administrador la resuelva");
        }

        RoleRemovalRequestEntity entity = new RoleRemovalRequestEntity();
        entity.setUser(user);
        entity.setRole(role);
        entity.setReason(request.getReason().trim());
        entity.setStatus(RequestStatus.PENDING);
        RoleRemovalRequestEntity saved = requestRepository.save(entity);
        log.info("Cuenta {} pidió quitar el rol {}", userId, role);
        notifyRoleRemoval(ACTION_REQUESTED, saved);
        return toDTO(saved);
    } // Cierre del método

    /**
     * Método que obtiene las peticiones de un usuario.
     *
     * @param userId El id del usuario autenticado, tomado del sub del token.
     * @return List<RoleRemovalResponseDTO> Sus peticiones, las más recientes primero.
     */
    @Transactional(readOnly = true)
    public List<RoleRemovalResponseDTO> findOwn(UUID userId) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDTO).toList();
    } // Cierre del método

    /**
     * Método que obtiene las peticiones para el administrador, con filtros opcionales.
     *
     * @param status El estado por el que se filtra, o null.
     * @param userId La cuenta por la que se filtra, o null.
     * @return List<RoleRemovalResponseDTO> Las peticiones filtradas, las más recientes primero.
     */
    @Transactional(readOnly = true)
    public List<RoleRemovalResponseDTO> findAll(RequestStatus status, UUID userId) {
        List<RoleRemovalRequestEntity> requests;
        if (userId != null) {
            requests = requestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .filter(request -> status == null || request.getStatus() == status)
                    .toList();
        } else if (status != null) {
            requests = requestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            requests = requestRepository.findAllByOrderByCreatedAtDesc();
        }
        return requests.stream().map(this::toDTO).toList();
    } // Cierre del método

    /**
     * Método que aprueba una petición pendiente: quita el rol y la marca APPROVED. Si quitar el rol falla, la
     * petición sigue pendiente.
     *
     * @param adminId El id del administrador autenticado, tomado del sub del token.
     * @param requestId El id de la petición.
     * @param adminReason El motivo del administrador.
     * @return RoleRemovalResponseDTO La petición aprobada.
     */
    public RoleRemovalResponseDTO approve(UUID adminId, UUID requestId, String adminReason) {
        RoleRemovalRequestEntity request = findPending(requestId);
        userService.removeRole(adminId, request.getUser().getId(), request.getRole());
        resolve(request, RequestStatus.APPROVED, adminReason);
        notifyRoleRemoval(ACTION_APPROVED, request);
        log.info("Admin {} aprobó la petición {}: rol {} quitado de la cuenta {}. Motivo: {}",
                adminId, requestId, request.getRole(), request.getUser().getId(), adminReason);
        return toDTO(request);
    } // Cierre del método

    /**
     * Método que rechaza una petición pendiente sin quitar el rol.
     *
     * @param adminId El id del administrador autenticado, tomado del sub del token.
     * @param requestId El id de la petición.
     * @param adminReason El motivo del administrador.
     * @return RoleRemovalResponseDTO La petición rechazada.
     */
    public RoleRemovalResponseDTO reject(UUID adminId, UUID requestId, String adminReason) {
        RoleRemovalRequestEntity request = findPending(requestId);
        resolve(request, RequestStatus.REJECTED, adminReason);
        notifyRoleRemoval(ACTION_REJECTED, request);
        log.info("Admin {} rechazó la petición {}. Motivo: {}", adminId, requestId, adminReason);
        return toDTO(request);
    } // Cierre del método

    /**
     * Método con el que el administrador quita un rol directamente, sin motivo. Si había una petición
     * pendiente para ese rol, queda aprobada con un motivo fijo y se avisa al usuario por correo.
     *
     * @param adminId El id del administrador autenticado, tomado del sub del token.
     * @param userId El id de la cuenta.
     * @param role El rol a quitar.
     * @return UserResponseDTO La cuenta completa sin el rol.
     */
    public UserResponseDTO removeByAdmin(UUID adminId, UUID userId, RoleName role) {
        UserResponseDTO updated = userService.removeRole(adminId, userId, role);
        requestRepository.findFirstByUserIdAndRoleAndStatus(userId, role, RequestStatus.PENDING)
                .ifPresent(request -> {
                    resolve(request, RequestStatus.APPROVED, DIRECT_REMOVAL_REASON);
                    notifyRoleRemoval(ACTION_APPROVED, request);
                });
        return updated;
    } // Cierre del método

    /**
     * Método que busca una petición y verifica que siga pendiente.
     *
     * @param requestId El id de la petición.
     * @return RoleRemovalRequestEntity La petición, para que no se resuelva dos veces.
     */
    private RoleRemovalRequestEntity findPending(UUID requestId) {
        RoleRemovalRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Petición no encontrada: " + requestId));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("La petición ya fue resuelta (" + request.getStatus() + ")");
        }
        return request;
    } // Cierre del método

    /**
     * Método que marca la petición como resuelta y la guarda.
     *
     * @param request La petición.
     * @param status El estado final (APPROVED o REJECTED).
     * @param adminReason El motivo del administrador.
     */
    private void resolve(RoleRemovalRequestEntity request, RequestStatus status, String adminReason) {
        request.setStatus(status);
        request.setAdminReason(adminReason.trim());
        request.setResolvedAt(LocalDateTime.now());
        requestRepository.save(request);
    } // Cierre del método

    /**
     * Método que le pide a Servicios el correo de una petición. En REQUESTED el destinatario es el
     * administrador (admin.email) y en APPROVED y REJECTED es el usuario; solo se envía el correo del
     * destinatario. Si falta admin.email no se avisa y queda en el log.
     *
     * @param action La acción: REQUESTED, APPROVED o REJECTED.
     * @param request La petición ya guardada (en APPROVED y REJECTED, con el motivo del administrador).
     */
    private void notifyRoleRemoval(String action, RoleRemovalRequestEntity request) {
        boolean toAdmin = ACTION_REQUESTED.equals(action);
        if (toAdmin && (adminEmail == null || adminEmail.isBlank())) {
            log.warn("ADMIN_NOTIFICATION_EMAIL no está configurado: no se avisa al administrador de la petición {}",
                    request.getId());
            return;
        }
        UserEntity user = request.getUser();
        RoleRemovalNotification notification = new RoleRemovalNotification(action, request.getId(), user.getId(),
                request.getRole().name(), user.getName(), request.getReason(), request.getAdminReason(),
                toAdmin ? null : user.getEmail(), toAdmin ? adminEmail.trim() : null);
        afterCommit(() -> requestsClient.notifyRoleRemoval(notification));
    } // Cierre del método

    /**
     * Método que ejecuta una tarea cuando la transacción se confirma, para no mandar correos de decisiones que
     * luego se deshacen. Si no hay transacción activa, la ejecuta de inmediato.
     *
     * @param task La tarea a ejecutar.
     */
    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                } // Cierre del método
            });
        } else {
            task.run();
        }
    } // Cierre del método

    /**
     * Método que verifica si una cuenta tiene un rol.
     *
     * @param user La cuenta.
     * @param role El rol buscado.
     * @return boolean Verifica si la cuenta tiene el rol.
     */
    private static boolean hasRole(UserEntity user, RoleName role) {
        return user.getRoles().stream().anyMatch(r -> r.getName().equals(role.name()));
    } // Cierre del método

    /**
     * Método que convierte una petición a DTO.
     *
     * @param entity La petición.
     * @return RoleRemovalResponseDTO La petición con el nombre del usuario, para el front.
     */
    private RoleRemovalResponseDTO toDTO(RoleRemovalRequestEntity entity) {
        RoleRemovalResponseDTO dto = new RoleRemovalResponseDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setUserName(entity.getUser().getName());
        dto.setRole(entity.getRole());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setAdminReason(entity.getAdminReason());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setResolvedAt(entity.getResolvedAt());
        return dto;
    } // Cierre del método
} // Cierre de la clase