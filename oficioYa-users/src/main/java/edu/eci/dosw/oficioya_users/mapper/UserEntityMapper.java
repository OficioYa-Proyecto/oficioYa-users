package edu.eci.dosw.oficioya_users.mapper;

import edu.eci.dosw.oficioya_users.entity.LocalityEntity;
import edu.eci.dosw.oficioya_users.entity.PhoneEntity;
import edu.eci.dosw.oficioya_users.entity.RoleEntity;
import edu.eci.dosw.oficioya_users.entity.UserEntity;
import edu.eci.dosw.oficioya_users.model.LocalityModel;
import edu.eci.dosw.oficioya_users.model.PhoneModel;
import edu.eci.dosw.oficioya_users.model.RoleModel;
import edu.eci.dosw.oficioya_users.model.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Interfaz de MapStruct que convierte entre entidades JPA
 * y modelos de dominio (usuario, teléfono, rol y localidad)
 * [usado por UserService]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

@Mapper(componentModel = "spring")
public interface UserEntityMapper {
    /**
     * Método que convierte una cuenta de entidad a modelo
     *
     * @param entity La entidad
     * @return UserModel El modelo para aplicarle las reglas de negocio
     */
    UserModel toModel(UserEntity entity);

    /**
     * Método que convierte una cuenta de modelo a entidad
     *
     * @param model El modelo
     * @return UserEntity La entidad para guardarla con JPA
     */
    UserEntity toEntity(UserModel model);

    /**
     * Método que convierte un teléfono de entidad a modelo
     *
     * @param entity La entidad
     * @return PhoneModel El modelo del teléfono
     */
    PhoneModel toModel(PhoneEntity entity);

    /**
     * Método que convierte un teléfono de modelo a entidad.
     * Ignora user para evitar un ciclo
     *
     * @param model El modelo
     * @return PhoneEntity La entidad del teléfono
     */
    @Mapping(target = "user", ignore = true)
    PhoneEntity toEntity(PhoneModel model);

    /**
     * Método que convierte un rol de entidad a modelo
     *
     * @param entity La entidad
     * @return RoleModel El modelo del rol
     */
    RoleModel toModel(RoleEntity entity);

    /**
     * Método que convierte un rol de modelo a entidad
     *
     * @param model El modelo
     * @return RoleEntity La entidad del rol
     */
    RoleEntity toEntity(RoleModel model);

    /**
     * Método que convierte una localidad de entidad a modelo
     *
     * @param entity La entidad
     * @return LocalityModel El modelo de la localidad
     */
    LocalityModel toModel(LocalityEntity entity);

    /**
     * Método que convierte una localidad de modelo a entidad
     *
     * @param model El modelo
     * @return LocalityEntity La entidad de la localidad
     */
    LocalityEntity toEntity(LocalityModel model);
} // Cierre de la interfaz