package edu.eci.dosw.oficioya_users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** RF-14. Localidad de Bogotá del catálogo de localidades. Ids fijos (data.sql) porque otros microservicios los usan */
@Getter
@Setter
@Entity
@Table(name = "localities")
public class LocalityEntity {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}