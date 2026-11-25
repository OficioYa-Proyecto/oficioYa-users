package edu.eci.dosw.oficioya_users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Clase entidad de la tabla localities: localidad de Bogotá del catálogo (RF-14).
 * Los ids son fijos porque otros microservicios los usan.
 * Se relaciona con UserEntity (una localidad, muchos usuarios)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@Getter
@Setter
@Entity
@Table(name = "localities")
public class LocalityEntity {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
} // Cierre de la clase