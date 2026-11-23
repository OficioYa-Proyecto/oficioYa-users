package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.RoleRemovalRequestEntity;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRemovalRequestRepository extends JpaRepository<RoleRemovalRequestEntity, UUID> {

    boolean existsByUserIdAndStatus(UUID userId, RequestStatus status);

    Optional<RoleRemovalRequestEntity> findFirstByUserIdAndRoleAndStatus(UUID userId, RoleName role, RequestStatus status);

    List<RoleRemovalRequestEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<RoleRemovalRequestEntity> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<RoleRemovalRequestEntity> findAllByOrderByCreatedAtDesc();
}