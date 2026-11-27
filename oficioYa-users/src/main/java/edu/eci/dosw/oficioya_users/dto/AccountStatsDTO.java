package edu.eci.dosw.oficioya_users.dto;

/**
 * Record con las estadísticas de cuentas que pide el
 * microservicio de Reporting (RF-12)
 *
 * @param registered Las cuentas registradas hasta la fecha de fin, activas o no
 * @param deactivated Las cuentas desactivadas dentro del periodo
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
public record AccountStatsDTO(RoleCountDTO registered, RoleCountDTO deactivated) {
} // Cierre del record
