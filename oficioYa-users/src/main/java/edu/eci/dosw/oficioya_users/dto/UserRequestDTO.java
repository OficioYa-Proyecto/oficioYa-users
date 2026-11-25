package edu.eci.dosw.oficioya_users.dto;

import edu.eci.dosw.oficioya_users.enums.RoleName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Clase DTO con los datos del registro de una cuenta (RF-01), (RF-14).
 * Las anotaciones validan el formato y las reglas de negocio
 * las valida UserService
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@Data
public class UserRequestDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
    private String name;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo admite máximo 150 caracteres")
    private String email;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate birthDate;

    @NotNull(message = "La localidad es obligatoria")
    private Long localityId;

    @NotEmpty(message = "Debe registrar al menos un teléfono")
    @Size(max = 3, message = "Se admiten máximo 3 teléfonos")
    private List<@Valid PhoneDTO> phones;

    @NotEmpty(message = "Debe elegir al menos un rol")
    private Set<RoleName> roles;
} // Cierre de la clase