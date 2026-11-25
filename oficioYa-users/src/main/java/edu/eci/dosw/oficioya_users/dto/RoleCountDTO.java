package edu.eci.dosw.oficioya_users.dto;

/**
 * Record con la cantidad de cuentas en total y por rol (RF-12).
 * Una cuenta con ambos roles cuenta en los dos,
 * por eso el total puede ser menor que la suma de trabajador y contratante
 *
 * @param total Las cuentas con algún rol de cuenta
 * @param trabajador Las cuentas con rol TRABAJADOR
 * @param contratante Las cuentas con rol CONTRATANTE
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
public record RoleCountDTO(long total, long trabajador, long contratante) {
} // Cierre del record
