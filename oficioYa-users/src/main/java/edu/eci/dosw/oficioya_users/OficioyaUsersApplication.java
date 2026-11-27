package edu.eci.dosw.oficioya_users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Clase principal que arranca el microservicio de Users con SpringBoot
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OficioyaUsersApplication {

    /**
     * Método que inicia la aplicación
     *
     * @param args Los argumentos de los comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(OficioyaUsersApplication.class, args);
    }
}
