package edu.eci.dosw.oficioya_users.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase de dominio de una localidad (id y nombre)
 * sin anotaciones JPA [convertida con UserEntityMapper]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

@Getter
@Setter
public class LocalityModel {
    private Long id;
    private String name;
} // Cierre de la clase