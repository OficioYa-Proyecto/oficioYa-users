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

/**
 * Interfaz JPA de las cuentas
 * [usado por UserService, RoleRemovalService y AdminSeeder]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    /**
     * Método que verifica si ya existe una cuenta con un correo
     *
     * @param email El correo normalizado.
     * @return boolean Verifica si existe para no repetir correos.
     */
    boolean existsByEmail(String email);

    /**
     * Método que busca una cuenta por correo sin importar su estado
     *
     * @param email El correo normalizado.
     * @return Optional<UserEntity> La cuenta, para la
     * consulta interna del microservicio de Identity (RF-11).
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Método que busca una cuenta por id en un estado
     *
     * @param id El id de la cuenta
     * @param status El estado de la cuenta
     * @return Optional<UserEntity> La cuenta para mostrar solo perfiles públicos activos
     */
    Optional<UserEntity> findByIdAndAccountStatus(UUID id, AccountStatus status);

    /**
     * Método que busca varias cuentas por id en un estado
     *
     * @param ids Los ids de las cuentas
     * @param status El estado de la cuenta
     * @return List<UserEntity> Las cuentas encontradas para
     * la consulta de varios usuarios (RF-10)
     */
    List<UserEntity> findByIdInAndAccountStatus(Collection<UUID> ids, AccountStatus status);

    /**
     * Método que cuenta los perfiles con alguno de los roles creados
     * antes de una fecha, activas o no (RF-12)
     *
     * @param roles Los nombres de los roles
     * @param end La fecha límite (excluida)
     * @return long La cantidad de cuentas registradas
     */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM UserEntity u JOIN u.roles r
            WHERE r.name IN :roles AND u.createdAt < :end""")
    long countRegistered(@Param("roles") Collection<String> roles, @Param("end") LocalDateTime end);

    /**
     * Método que cuenta los perfiles con alguno de los roles
     * desactivadas en un periodo (RF-12)
     *
     * @param roles Los nombres de los roles
     * @param start El inicio del periodo (incluido)
     * @param end El fin del periodo (excluido)
     * @return long La cantidad de cuentas desactivadas
     */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM UserEntity u JOIN u.roles r
            WHERE r.name IN :roles AND u.deactivatedAt >= :start AND u.deactivatedAt < :end""")
    long countDeactivated(@Param("roles") Collection<String> roles,
                          @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
} // Cierre de la Interfaz