package edu.eci.dosw.oficioya_users.mapper;

import edu.eci.dosw.oficioya_users.dto.LocalityDTO;
import edu.eci.dosw.oficioya_users.dto.PhoneDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.model.PhoneModel;
import edu.eci.dosw.oficioya_users.model.RoleModel;
import edu.eci.dosw.oficioya_users.model.UserModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Clase que convierte entre modelos y DTOs según quien consulta:
 * vista pública, propia (con datos ocultos) o del admin (datos
 * compeltos) [usado por UserService]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

@Component
public class UserMapper {
    /**
     * Método que convierte los teléfonos recibidos a modelos
     * (solo número y principal)
     *
     * @param dtos Los teléfonos de la petición
     * @return List<PhoneModel> Los modelos para guardarlos en la cuenta
     */
    public List<PhoneModel> toPhones(List<PhoneDTO> dtos) {
        return dtos.stream().map(dto -> {
            PhoneModel phone = new PhoneModel();
            phone.setNumber(dto.getNumber());
            phone.setPrincipal(dto.isPrincipal());
            return phone;
        }).toList();
    } // Cierre del método

    /**
     * Método que arma la vista pública (RF-05): id, nombre, roles y localidad
     *
     * @param user La cuenta
     * @return UserResponseDTO La cuenta sin datos de contacto para que otros la vean
     */
    public UserResponseDTO toPublic(UserModel user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setRoles(user.getRoles().stream().map(RoleModel::getName).collect(Collectors.toSet()));
        if (user.getLocality() != null) {
            dto.setLocality(new LocalityDTO(user.getLocality().getId(), user.getLocality().getName()));
        }
        return dto;
    } // Cierre del método

    /**
     * Método que arma la vista propia (RF-03): lo público más correo y teléfonos ocultos, fecha de
     * nacimiento, estado y fecha de creación
     *
     * @param user La cuenta
     * @return UserResponseDTO El perfil del dueño, sin exponer los datos completos de contacto
     */
    public UserResponseDTO toOwn(UserModel user) {
        UserResponseDTO dto = toPublic(user);
        dto.setEmail(maskEmail(user.getEmail()));
        dto.setBirthDate(user.getBirthDate());
        dto.setPhones(user.getPhones().stream().map(p -> phoneDTO(p.getId(), maskPhone(p.getNumber()), p.isPrincipal())).toList());
        dto.setAccountStatus(user.getAccountStatus());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    } // Cierre del método

    /**
     * Método que arma la vista del admin (RF-08): todos los datos sin ocultar y las fechas
     *
     * @param user La cuenta
     * @return UserResponseDTO La cuenta completa para el admin y las consultas internas
     */
    public UserResponseDTO toAdmin(UserModel user) {
        UserResponseDTO dto = toPublic(user);
        dto.setEmail(user.getEmail());
        dto.setBirthDate(user.getBirthDate());
        dto.setPhones(toPhoneDTOs(user.getPhones()));
        dto.setAccountStatus(user.getAccountStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    } // Cierre del método

    /**
     * Método que convierte los teléfonos a DTO sin ocultar
     *
     * @param phones Los teléfonos de la cuenta
     * @return List<PhoneDTO> Los teléfonos completos para el admin y para que el dueño los edite
     */
    public List<PhoneDTO> toPhoneDTOs(List<PhoneModel> phones) {
        return phones.stream().map(p -> phoneDTO(p.getId(), p.getNumber(), p.isPrincipal())).toList();
    } // Cierre del método

    /**
     * Método que crea el DTO de un teléfono
     *
     * @param id El id del teléfono
     * @param number El número (completo u oculto)
     * @param principal Si es el teléfono principal
     * @return PhoneDTO El teléfono listo para la respuesta
     */
    private PhoneDTO phoneDTO(UUID id, String number, boolean principal) {
        PhoneDTO dto = new PhoneDTO();
        dto.setId(id);
        dto.setNumber(number);
        dto.setPrincipal(principal);
        return dto;
    } // Cierre del método

    /**
     * Método que oculta el correo dejando la primera y la última letra antes de la @
     * (carlos@gmail.com -> c**s@gmail.com)
     *
     * @param email El correo completo
     * @return String El correo oculto
     */
    static String maskEmail(String email) {
        int at = email.indexOf('@');
        String user = email.substring(0, at);
        String last = user.length() > 1 ? user.substring(user.length() - 1) : "";
        return user.charAt(0) + "**" + last + email.substring(at);
    } // Cierre del método

    /**
     * Método que oculta un teléfono dejando visibles los últimos 4 dígitos
     *
     * @param number El número completo
     * @return String El número oculto
     */
    static String maskPhone(String number) {
        int visible = Math.min(4, number.length());
        return "*".repeat(number.length() - visible) + number.substring(number.length() - visible);
    } // Cierre del método
} // Cierre de la clase