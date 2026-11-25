package edu.eci.dosw.oficioya_users.entity;

import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase entidad de la tabla role_removal_requests: petición de un usuario para que el admin le quite
 * un rol (RF-13).Pertenece a un UserEntity y guarda la razón,
 * el estado, el motivo del admin y las fechas
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
@Table(name = "role_removal_requests")
public class RoleRemovalRequestEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private RoleName role;

    /** Razón del usuario */
    @Column(nullable = false, updatable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    /** Motivo del administrador al aprobar o rechazar */
    @Column(length = 500)
    private String adminReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    /**
     * Método que se ejecuta antes de guardar la petición por primera vez:
     * pone la fecha de creación y si no tiene estado, la deja en PENDING
     */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    } // Cierre del método
} // Cierre de la clase