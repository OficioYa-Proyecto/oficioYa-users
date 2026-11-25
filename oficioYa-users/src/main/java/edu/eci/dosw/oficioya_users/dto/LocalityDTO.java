package edu.eci.dosw.oficioya_users.dto;

/**
 * Record que representa una localidad del catálogo en las respuestas (RF-14)
 *
 * @param id El id fijo de la localidad
 * @param name El nombre de la localidad
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
public record LocalityDTO(Long id, String name) {
} // Cierre del record