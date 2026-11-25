package edu.eci.dosw.oficioya_users;

import edu.eci.dosw.oficioya_users.entity.RoleEntity;
import edu.eci.dosw.oficioya_users.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Clase de prueba que levanta todo el contexto de Spring Boot con la base de datos de pruebas.
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@SpringBootTest
class OficioyaUsersApplicationTests {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Prueba que la aplicación arranca y los roles TRABAJADOR, CONTRATANTE y ADMIN quedan sembrados
     */
    @Test
    void contextLoadsAndSeedsRoles() {
        assertThat(roleRepository.findAll()).extracting(RoleEntity::getName)
                .containsExactlyInAnyOrder("TRABAJADOR", "CONTRATANTE", "ADMIN");
    } // Cierre del método
} // Cierre de la clase
