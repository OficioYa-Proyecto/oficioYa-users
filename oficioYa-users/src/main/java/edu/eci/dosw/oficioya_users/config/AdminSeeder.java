package edu.eci.dosw.oficioya_users.config;

import edu.eci.dosw.oficioya_users.entity.PhoneEntity;
import edu.eci.dosw.oficioya_users.entity.RoleEntity;
import edu.eci.dosw.oficioya_users.entity.UserEntity;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.repository.RoleRepository;
import edu.eci.dosw.oficioya_users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Clase de configuración que crea el administrador inicial (admin semilla) a partir de las variables de
 * entorno ADMIN_EMAIL, ADMIN_NAME y ADMIN_PHONE. Solo se carga si admin.email tiene valor.
 * [usa UserRepository y RoleRepository]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@Slf4j
@Configuration
@ConditionalOnExpression("!'${admin.email:}'.isBlank()")
public class AdminSeeder {
    /**
     * Método que define la tarea que crea el admin al arrancar la aplicación. Si ya existe una cuenta
     * con ese correo no hace nada, si falta el teléfono o el rol ADMIN no está, falla
     *
     * @param userRepository El repositorio de usuarios
     * @param roleRepository El repositorio de roles
     * @param email El correo del administrador (se normaliza en minúsculas)
     * @param name El nombre del administrador
     * @param phone El teléfono principal del admin
     * @return CommandLineRunner La tarea que Spring ejecuta al iniciar para que siempre exista un
     * admin
     */
    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, RoleRepository roleRepository,
                                @Value("${admin.email}") String email,
                                @Value("${admin.name}") String name,
                                @Value("${admin.phone}") String phone) {
        return args -> {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (userRepository.existsByEmail(normalized)) {
                return;
            }
            if (phone == null || phone.isBlank()) {
                throw new IllegalStateException("ADMIN_PHONE es obligatorio para crear el administrador");
            }
            RoleEntity adminRole = roleRepository.findByName(RoleName.ADMIN.name())
                    .orElseThrow(() -> new IllegalStateException("Rol no configurado: ADMIN"));

            PhoneEntity principal = new PhoneEntity();
            principal.setNumber(phone.trim());
            principal.setPrincipal(true);

            UserEntity admin = new UserEntity();
            admin.setName(name.trim());
            admin.setEmail(normalized);
            admin.setAccountStatus(AccountStatus.ACTIVE);
            admin.setPhones(List.of(principal));
            admin.setRoles(new HashSet<>(Set.of(adminRole)));

            log.info("Administrador semilla creado con id {}", userRepository.save(admin).getId());
        };
    } // Cierre del método
} // Cierre de la clase