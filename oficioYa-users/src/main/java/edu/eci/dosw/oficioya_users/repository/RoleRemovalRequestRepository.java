package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.RoleRemovalRequestEntity;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz JPA de las solicitudes para quitar un rol
 * [usado por RoleRemovalService]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public interface RoleRemovalRequestRepository extends JpaRepository<RoleRemovalRequestEntity, UUID> {
    /**
     * Método que verifica si una cuenta tiene una solicitud en un estado
     *
     * @param userId El id de la cuenta
     * @param status El estado buscado
     * @return boolean Verifica si existe para impedir dos solicitudes pendientes
     */
    boolean existsByUserIdAndStatus(UUID userId, RequestStatus status);

    /**
     * Método que busca la solicitud de una cuenta para un rol y un estado
     *
     * @param userId El id de la cuenta
     * @param role El rol
     * @param status El estado
     * @return Optional<RoleRemovalRequestEntity> La solicitud a aprobar cuando el admin quita el
     * rol directamente
     */
    Optional<RoleRemovalRequestEntity> findFirstByUserIdAndRoleAndStatus(UUID userId, RoleName role, RequestStatus status);

    /**
     * Método que obtiene las solicitudes de una cuenta
     *
     * @param userId El id de la cuenta
     * @return List<RoleRemovalRequestEntity> Las solicitudes más recientes primero
     */
    List<RoleRemovalRequestEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Método que obtiene las solicitudes en un estado
     *
     * @param status El estado
     * @return List<RoleRemovalRequestEntity> Las solicitudes más recientes primero
     */
    List<RoleRemovalRequestEntity> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /**
     * Método que obtiene todas las solicitudes
     *
     * @return List<RoleRemovalRequestEntity> Las solicitudes más recientes primero
     */
    List<RoleRemovalRequestEntity> findAllByOrderByCreatedAtDesc();
} // Cierre de la interfaz