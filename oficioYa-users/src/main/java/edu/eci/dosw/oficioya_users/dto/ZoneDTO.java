package edu.eci.dosw.oficioya_users.dto;

/**
 * Record que representa un barrio del catálogo de zonas (RF-09)
 *
 * @param id El id fijo del barrio
 * @param name El nombre del barrio
 * @param locality La localidad a la que pertenece el barrio
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
public record ZoneDTO(Long id, String name, String locality) {
} // Cierre del record
