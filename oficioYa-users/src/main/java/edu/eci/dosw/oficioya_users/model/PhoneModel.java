package edu.eci.dosw.oficioya_users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Clase de dominio de un teléfono (id, número y si
 * es principal) sin anotaciones JPA
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

@Getter
@Setter
public class PhoneModel {
    private UUID id;
    private String number;
    private boolean principal;
} // Cierre de la clase