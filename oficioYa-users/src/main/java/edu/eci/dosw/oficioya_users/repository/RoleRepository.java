package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz JPA de los roles
 * [usado por UserService y AdminSeeder]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    /**
     * Método que busca un rol por nombre
     *
     * @param name El nombre del rol
     * @return Optional<RoleEntity> El rol para asignarlo a una cuenta
     */
    Optional<RoleEntity> findByName(String name);
} // Cierre de la interfaz
