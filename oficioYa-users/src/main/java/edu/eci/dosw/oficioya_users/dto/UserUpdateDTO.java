package edu.eci.dosw.oficioya_users.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {
    @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
    @Pattern(regexp = "\\s*\\S.*", message = "El nombre no puede estar vacío")
    private String name;

    /** RF-14. Localidad (id del catálogo); la cambian el trabajador y el contratante, no el admin */
    private Long localityId;

    @Size(min = 1, max = 3, message = "Debe tener entre 1 y 3 teléfonos")
    private List<@Valid PhoneDTO> phones;
}