package edu.eci.dosw.oficioya_users.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RF-13. El usuario pide que le quiten un rol con una razón y solo el administrador lo quita:
 * aprobando la petición o directamente desde el detalle de la cuenta
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoleRemovalService {

    static final String DIRECT_REMOVAL_REASON = "El administrador quitó el rol desde el detalle de la cuenta";

    private final RoleRemovalRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    /** El usuario crea la petición; solo puede tener una pendiente */
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
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleRemovalResponseDTO> findOwn(UUID userId) {
        return requestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDTO).toList();
    }

    /** Para el admin: filtros opcionales por estado y por usuario; las más recientes primero */
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
    }

    /** Aprobar = quitar el rol (mismas reglas de UserService.removeRole); si falla, sigue pendiente */
    public RoleRemovalResponseDTO approve(UUID adminId, UUID requestId, String adminReason) {
        RoleRemovalRequestEntity request = findPending(requestId);
        userService.removeRole(adminId, request.getUser().getId(), request.getRole());
        resolve(request, RequestStatus.APPROVED, adminReason);
        log.info("Admin {} aprobó la petición {}: rol {} quitado de la cuenta {}. Motivo: {}",
                adminId, requestId, request.getRole(), request.getUser().getId(), adminReason);
        return toDTO(request);
    }

    public RoleRemovalResponseDTO reject(UUID adminId, UUID requestId, String adminReason) {
        RoleRemovalRequestEntity request = findPending(requestId);
        resolve(request, RequestStatus.REJECTED, adminReason);
        log.info("Admin {} rechazó la petición {}. Motivo: {}", adminId, requestId, adminReason);
        return toDTO(request);
    }

    /**
     * El admin quita el rol desde el detalle de la cuenta, sin motivo (el front no lo envía);
     * si había una petición pendiente para ese rol, queda aprobada con un motivo fijo
     */
    public UserResponseDTO removeByAdmin(UUID adminId, UUID userId, RoleName role) {
        UserResponseDTO updated = userService.removeRole(adminId, userId, role);
        requestRepository.findFirstByUserIdAndRoleAndStatus(userId, role, RequestStatus.PENDING)
                .ifPresent(request -> resolve(request, RequestStatus.APPROVED, DIRECT_REMOVAL_REASON));
        return updated;
    }

    private RoleRemovalRequestEntity findPending(UUID requestId) {
        RoleRemovalRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Petición no encontrada: " + requestId));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("La petición ya fue resuelta (" + request.getStatus() + ")");
        }
        return request;
    }

    private void resolve(RoleRemovalRequestEntity request, RequestStatus status, String adminReason) {
        request.setStatus(status);
        request.setAdminReason(adminReason.trim());
        request.setResolvedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    private static boolean hasRole(UserEntity user, RoleName role) {
        return user.getRoles().stream().anyMatch(r -> r.getName().equals(role.name()));
    }

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
    }
}