package edu.eci.dosw.oficioya_users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

/**
 * Clase DTO de un teléfono en las peticiones y respuestas.
 * Valida que el número sea un celular colombiano.
 * El id solo va en las respuestas, para poder editar o eliminar el teléfono.
 * Indica si es el teléfono principal de la cuenta.
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@Data
public class PhoneDTO {
    private UUID id;

    @NotBlank(message = "El número es obligatorio")
    @Pattern(regexp = "^3[0-9]{9}$", message = "Ingrese un teléfono válido")
    private String number;

    private boolean principal;
} // Cierre de la clase
