package edu.eci.dosw.oficioya_users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Clase entidad de la tabla zones: barrio del catálogo de zonas con su localidad. Los ids son fijos
 * porque el microservicio de Workers los guarda en sus trabajadores
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@Getter
@Setter
@Entity
@Table(name = "zones")
public class ZoneEntity {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String locality;
} // Cierre de la clase
