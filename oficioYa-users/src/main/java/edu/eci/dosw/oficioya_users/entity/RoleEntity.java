package edu.eci.dosw.oficioya_users.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Clase entidad de la tabla roles (ADMIN, CONTRATANTE, TRABAJADOR).
 * Se relaciona con UserEntity mediante la
 * tabla intermedia users_roles.
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
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
} // Cierre de la clase
