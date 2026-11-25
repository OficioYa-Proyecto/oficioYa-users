package edu.eci.dosw.oficioya_users.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración de la documentación OpenAPI (Swagger)
 * del microservicio de Users
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@Configuration
public class SwaggerConfig {
    /**
     * Método que define título, versión, descripción y
     * esquema de seguridad bearerAuth (JWT)
     *
     * @return OpenAPI La configuración de la documentación para poder probar los endpoints protegidos desde
     *         Swagger con el botón Authorize
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OficioYa - Microservicio de usuarios")
                        .version("1.0.0")
                        .description("Registro, perfiles y administración de cuentas"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    } // Cierre del método
} // Cierre de la clase