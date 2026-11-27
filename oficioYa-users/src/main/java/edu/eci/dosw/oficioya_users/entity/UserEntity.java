package edu.eci.dosw.oficioya_users.entity;

import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Clase entidad de la tabla users: la cuenta de usuario.
 * Tiene teléfonos, roles y una localidad. Guarda las fechas de creación,
 * actualización y desactivación (RF-12)
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
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, updatable = false, length = 150)
    private String email;

    /** RF-01. Nullable: las cuentas anteriores y el admin semilla no la tienen */
    @Column(updatable = false)
    private LocalDate birthDate;

    /** RF-14. Localidad; opcional; la tienen el trabajador y el contratante, no el admin */
    @ManyToOne
    @JoinColumn(name = "locality_id")
    private LocalityEntity locality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** Cuándo se desactivó la cuenta; null si está activa (RF-12) */
    private LocalDateTime deactivatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhoneEntity> phones = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    /**
     * Método que se ejecuta antes de guardar la cuenta por primera vez:
     * pone fechas de creación y actualización y si no tiene estado
     * la deja en ACTIVE
     */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
    } // Cierre del método

    /**
     * Método que reemplaza los teléfonos de la cuenta,
     * los que ya no están se borran de la base de datos
     *
     * @param newPhones Los teléfonos nuevos (si es nulo, la cuenta queda sin teléfonos)
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setPhones(List<PhoneEntity> newPhones) {
        phones.clear();
        if (newPhones != null) {
            newPhones.forEach(this::addPhone);
        }
    } // Cierre del método

    /**
     * Método que agrega un teléfono y lo enlaza con la cuenta
     * para que JPA guarde la llave foránea.
     *
     * @param phone El teléfono a agregar.
     */
    public void addPhone(PhoneEntity phone) {
        phone.setUser(this);
        phones.add(phone);
    } // Cierre del método
} // Cierre de la clase