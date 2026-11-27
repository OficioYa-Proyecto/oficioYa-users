package edu.eci.dosw.oficioya_users.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Clase entidad de la tabla phones: teléfono de una cuenta, con la marca de principal.
 * Muchos teléfonos pertenecen a un UserEntity
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
@Table(name = "phones")
public class PhoneEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(name = "is_principal", nullable = false)
    private boolean principal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
} // Cierre de la clase
