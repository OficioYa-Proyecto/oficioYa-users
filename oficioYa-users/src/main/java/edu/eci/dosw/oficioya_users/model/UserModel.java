package edu.eci.dosw.oficioya_users.model;

import edu.eci.dosw.oficioya_users.enums.AccountStatus;
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
 * Clase de dominio de una cuenta sin anotaciones JPA
 * UserService aplica reglas de negocio sobre ella para no depender
 * de la base  de datos
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

@Getter
@Setter
public class UserModel {
    private UUID id;
    private String name;
    private String email;
    private LocalDate birthDate;
    private LocalityModel locality;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deactivatedAt;
    private List<PhoneModel> phones = new ArrayList<>();
    private Set<RoleModel> roles = new HashSet<>();
} // Cierre de la clase