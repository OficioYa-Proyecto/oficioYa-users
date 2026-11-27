package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Interfaz JPA del catálogo de zonas
 * [usado por ZoneService]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

public interface ZoneRepository extends JpaRepository<ZoneEntity, Long> {
    /**
     * Método que obtiene todas las zonas
     *
     * @return List<ZoneEntity> Las zonas ordenadas por id
     */
    List<ZoneEntity> findAllByOrderByIdAsc();

    /**
     * Método que busca varias zonas por id
     *
     * @param ids Los ids de las zonas
     * @return List<ZoneEntity> Las zonas que existen ordenadas por id
     */
    List<ZoneEntity> findByIdInOrderByIdAsc(Collection<Long> ids);
} // Cierre de la interfaz
