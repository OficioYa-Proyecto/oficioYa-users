package edu.eci.dosw.oficioya_users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Clase de dominio de un rol (id y nombre) sin anotaciones JPA
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

@Getter
@Setter
public class RoleModel {
    private UUID id;
    private String name;
} // Cierre de la clase