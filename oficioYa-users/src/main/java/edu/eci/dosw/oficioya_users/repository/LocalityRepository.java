package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.LocalityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Interfaz JPA de las localidades.
 * [usado por LocalityService y UserService]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public interface LocalityRepository extends JpaRepository<LocalityEntity, Long> {
    /**
     * Método que obtiene todas las localidades
     *
     * @return List<LocalityEntity> Las localidades en orden alfabético para el catálogo
     */
    List<LocalityEntity> findAllByOrderByNameAsc();
} // Cierre de la interfaz