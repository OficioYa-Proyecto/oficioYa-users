package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.UserEntity;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByIdAndAccountStatus(UUID id, AccountStatus status);

    List<UserEntity> findByIdInAndAccountStatus(Collection<UUID> ids, AccountStatus status);

    /** RF-12. Cuentas con alguno de esos roles creadas, estén activas o no */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM UserEntity u JOIN u.roles r
            WHERE r.name IN :roles AND u.createdAt < :end""")
    long countRegistered(@Param("roles") Collection<String> roles, @Param("end") LocalDateTime end);

    /** RF-12. Cuentas con alguno de esos roles desactivadas entre 'start' (incluido) y 'end' (excluido) */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM UserEntity u JOIN u.roles r
            WHERE r.name IN :roles AND u.deactivatedAt >= :start AND u.deactivatedAt < :end""")
    long countDeactivated(@Param("roles") Collection<String> roles,
                          @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}