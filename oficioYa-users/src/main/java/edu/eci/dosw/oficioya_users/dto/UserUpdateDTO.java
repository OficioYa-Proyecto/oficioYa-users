package edu.eci.dosw.oficioya_users.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Clase DTO para editar el perfil propio (RF-04).
 * Todos los campos son opcionales: solo se cambia lo que se
 * envía (nombre, localidad y la lista completa de teléfonos)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@Data
public class UserUpdateDTO {
    @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
    @Pattern(regexp = "\\s*\\S.*", message = "El nombre no puede estar vacío")
    private String name;

    private Long localityId;

    @Size(min = 1, max = 3, message = "Debe tener entre 1 y 3 teléfonos")
    private List<@Valid PhoneDTO> phones;
} // Cierre de la clase