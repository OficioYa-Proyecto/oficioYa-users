package edu.eci.dosw.oficioya_users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Clase DTO de una cuenta en las respuestas.
 * Los campos nulos no se envían, por eso la misma clase sirve para
 * la vista pública, la propia y la del administrador
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private LocalDate birthDate;
    private LocalityDTO locality;
    private List<PhoneDTO> phones;
    private Set<String> roles;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} // Cierre de la clase