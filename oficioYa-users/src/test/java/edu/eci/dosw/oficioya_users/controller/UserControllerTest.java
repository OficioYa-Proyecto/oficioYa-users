package edu.eci.dosw.oficioya_users.controller;

import edu.eci.dosw.oficioya_users.dto.AccountStatsDTO;
import edu.eci.dosw.oficioya_users.dto.RoleCountDTO;
import edu.eci.dosw.oficioya_users.dto.UserRequestDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.dto.UserUpdateDTO;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.exception.ConflictException;
import edu.eci.dosw.oficioya_users.exception.ResourceNotFoundException;
import io.jsonwebtoken.io.Decoders;
import edu.eci.dosw.oficioya_users.security.SecurityConfig;
import edu.eci.dosw.oficioya_users.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Clase de pruebas de UserController con la seguridad real (SecurityConfig) y el servicio simulado: permisos
 * por rol, uso del id del token y formato de los errores
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=" + UserControllerTest.SECRET)
class UserControllerTest {
    static final String SECRET = "IadhiSp/AVyloXuE15l29oZzpucys4NEfqpHn6PEMeQ=";
    private static final String AUTH = "Authorization";
    private static final String STATS = "/api/v1/users/stats/accounts";
    private static final String REGISTER_BODY = """
            {"name":"Ana","email":"ana@mail.com","birthDate":"1995-05-20","localityId":1,
             "phones":[{"number":"3001234567","principal":true}],"roles":["TRABAJADOR"]}""";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;

    private final UUID userId = UUID.randomUUID();

    /**
     * Método que firma un token JWT de prueba.
     *
     * @param subject El sub del token.
     * @param roles Los roles.
     * @param secret El secreto en Base64.
     * @param ttlMillis La duración en milisegundos (negativa para un token expirado).
     * @return String El encabezado "Bearer <token>".
     */
    private static String bearer(String subject, List<String> roles, String secret, long ttlMillis) {
        return "Bearer " + Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    } // Cierre del método

    /**
     * Método que firma un token válido para el usuario de la prueba.
     *
     * @param roles Los roles del token.
     * @return String El encabezado "Bearer <token>".
     */
    private String token(String... roles) {
        return bearer(userId.toString(),List.of(roles), SECRET, 60_000);
    } // Cierre del método

    /**
     * Método que crea una cuenta de respuesta de ejemplo.
     *
     * @param name El nombre.
     * @return UserResponseDTO La cuenta, para simular el servicio.
     */
    private static UserResponseDTO response(String name) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setName(name);
        return dto;
    } // Cierre del método

    /**
     * Prueba que el registro solo lo puede hacer el rol SERVICE.
     */
    @Test
    void registerRequiresServiceRole() throws Exception {
        when(userService.register(any(UserRequestDTO.class))).thenReturn(response("Ana"));

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/users").header(AUTH, token("TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users").header(AUTH, token("SERVICE"))
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.email").doesNotExist());
    } // Cierre del método

    /**
     * Prueba que la eliminación (compensación) solo la puede hacer el rol SERVICE.
     */
    @Test
    void deleteOnlyAllowedForService() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/users/{id}", id).header(AUTH, token("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
        mockMvc.perform(delete("/api/v1/users/{id}", id).header(AUTH, token("SERVICE")))
                .andExpect(status().isNoContent());
    } // Cierre del método

    /**
     * Prueba que las estadísticas solo las puede consultar el administrador.
     */
    @Test
    void accountStatsOnlyAllowedForAdmin() throws Exception {
        when(userService.getAccountStats(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(new AccountStatsDTO(new RoleCountDTO(1480, 620, 900), new RoleCountDTO(40, 15, 28)));

        mockMvc.perform(get(STATS).param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(STATS).param("from", "2026-11-01").param("to", "2026-11-30").header(AUTH, token("SERVICE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(STATS).param("from", "2026-11-01").param("to", "2026-11-30").header(AUTH, token("CONTRATANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(STATS).param("from", "2026-11-01").param("to", "2026-11-30").header(AUTH, token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered.total").value(1480))
                .andExpect(jsonPath("$.registered.trabajador").value(620))
                .andExpect(jsonPath("$.registered.contratante").value(900))
                .andExpect(jsonPath("$.deactivated.total").value(40))
                .andExpect(jsonPath("$.deactivated.trabajador").value(15))
                .andExpect(jsonPath("$.deactivated.contratante").value(28));
    } // Cierre del método

    /**
     * Prueba que las estadísticas rechazan fechas con formato inválido (400).
     */
    @Test
    void accountStatsRejectsDateWithWrongFormat() throws Exception {
        mockMvc.perform(get(STATS).param("from", "01/11/2026").param("to", "2026-11-30").header(AUTH, token("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'from'"));
        verifyNoInteractions(userService);
    } // Cierre del método

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void invalidTokensReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(AUTH, "Bearer token.invalido"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header(AUTH, bearer(userId.toString(), List.of("ADMIN"), SECRET, -60_000)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me")
                        .header(AUTH, bearer(userId.toString(), List.of("ADMIN"), "rxl3eEuJLIOl0IYbwSy6RvQbtxX5ZahEsooK5QPmol0=", 60_000)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header(AUTH, bearer("no-es-uuid", List.of("ADMIN"), SECRET, 60_000)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header(AUTH, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header(AUTH, "Bearer " + Jwts.builder()
                        .subject(userId.toString())
                        .claim("roles","ADMIN")
                        .expiration(new Date(System.currentTimeMillis() + 60_000))
                        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                        .compact()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void tokenWithNonUuidSubjectReturns401() throws Exception {
        String token = bearer("123", List.of("CONTRATANTE"),  SECRET, 60_000);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Prueba que las consultas internas por correo y por id aceptan el rol SERVICE.
     */
    @Test
    void accountQueriesAllowServiceRole() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponseDTO dto = response("Ana");
        dto.setEmail("ana@mail.com");
        dto.setAccountStatus(AccountStatus.INACTIVE);
        when(userService.findByEmail("ana@mail.com")).thenReturn(dto);
        when(userService.findById(id)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/users/by-email").param("email", "ana@mail.com").header(AUTH, token("SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.accountStatus").value("INACTIVE"));
        mockMvc.perform(get("/api/v1/users/{id}/account", id).header(AUTH, token("SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("INACTIVE"));
    } // Cierre del método

    /**
     * Prueba que las consultas internas son solo para el rol SERVICE (403).
     */
    @Test
    void accountQueriesAreOnlyForService() throws Exception {
        UUID id = UUID.randomUUID();
        for (String role : List.of("ADMIN", "CONTRATANTE", "TRABAJADOR")) {
            mockMvc.perform(get("/api/v1/users/by-email").param("email", "ana@mail.com").header(AUTH, token(role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/users/{id}/account", id).header(AUTH, token(role)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/users/by-email").param("email", "ana@mail.com"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    } // Cierre del método

    @Test
    void accountQueryOfMissingUserReturns404() throws Exception {
        when(userService.findByEmail("nadie@mail.com")).thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        mockMvc.perform(get("/api/v1/users/by-email").param("email", "nadie@mail.com").header(AUTH, token("SERVICE")))
                .andExpect(status().isNotFound());
    }

    /**
     * Prueba que un usuario que no es administrador no puede usar las rutas de administración.
     */
    @Test
    void nonAdminCannotUseAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").header(AUTH, token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").header(AUTH, "Bearer " + Jwts.builder()
                        .subject(userId.toString())
                        .expiration(new Date(System.currentTimeMillis() + 60_000))
                        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                        .compact()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    } // Cierre del método

    @Test
    void registerWithInvalidDataReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/users").header(AUTH, token("SERVICE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"no-es-correo\",\"phones\":[],\"roles\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    /**
     * Prueba que el perfil propio usa el id que viene en el token.
     */
    @Test
    void ownProfileUsesIdFromToken() throws Exception {
        when(userService.getOwnProfile(userId)).thenReturn(response("Ana"));

        mockMvc.perform(get("/api/v1/users/me").header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"));
    } // Cierre del método

    /**
     * Prueba que la edición del perfil propio responde el perfil actualizado.
     */
    @Test
    void updateOwnProfile() throws Exception {
        when(userService.updateOwnProfile(eq(userId), any(UserUpdateDTO.class))).thenReturn(response("Ana María"));

        mockMvc.perform(patch("/api/v1/users/me").header(AUTH, token("TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana María\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana María"));
    } // Cierre del método

    /**
     * Prueba que agregar un rol usa el id del token.
     */
    @Test
    void addRole() throws Exception {
        when(userService.addRole(userId, RoleName.CONTRATANTE)).thenReturn(response("Ana"));

        mockMvc.perform(post("/api/v1/users/me/roles/CONTRATANTE").header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isOk());
    } // Cierre del método

    /**
     * Prueba que el perfil público se consulta con token.
     */
    @Test
    void publicProfile() throws Exception {
        UUID otherId = UUID.randomUUID();
        when(userService.getPublicProfile(userId, otherId)).thenReturn(response("Luis"));

        mockMvc.perform(get("/api/v1/users/{id}", otherId).header(AUTH, token("CONTRATANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luis"));
    } // Cierre del método

    /**
     * Prueba que la consulta de varios usuarios acepta el rol SERVICE.
     */
    @Test
    void usersByIdsAllowsServiceRole() throws Exception {
        UUID otherId = UUID.randomUUID();
        when(userService.findPublicByIds(List.of(otherId))).thenReturn(List.of(response("Luis")));

        mockMvc.perform(get("/api/v1/users").param("ids", otherId.toString()).header(AUTH, token("SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Luis"));
    } // Cierre del método

    @Test
    void usersByIdsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("ids", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void usersByIdsWithInvalidIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("ids", "uno").header(AUTH, token("SERVICE")))
                .andExpect(status().isBadRequest());
    }

    /**
     * Prueba que el administrador lista y consulta cuentas.
     */
    @Test
    void adminListsAndSeesUsers() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findAll()).thenReturn(List.of(response("Ana")));
        when(userService.findById(id)).thenReturn(response("Luis"));

        mockMvc.perform(get("/api/v1/admin/users").header(AUTH, token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana"));
        mockMvc.perform(get("/api/v1/admin/users/{id}", id).header(AUTH, token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luis"));
    } // Cierre del método

    /**
     * Prueba que un rol que ya trae el prefijo ROLE_ se acepta igual.
     */
    @Test
    void roleWithPrefixIsAcceptedAsIs() throws Exception {
        when(userService.findAll()).thenReturn(List.of(response("Ana")));

        mockMvc.perform(get("/api/v1/admin/users").header(AUTH, token("ROLE_ADMIN")))
                .andExpect(status().isOk());
    } // Cierre del método

    /**
     * Prueba que el administrador desactiva y activa cuentas enviando un motivo.
     */
    @Test
    void adminDeactivatesAndActivatesWithReason() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponseDTO inactive = response("Ana");
        inactive.setAccountStatus(AccountStatus.INACTIVE);
        UserResponseDTO active = response("Ana");
        active.setAccountStatus(AccountStatus.ACTIVE);
        when(userService.changeStatus(userId, id, AccountStatus.INACTIVE, "Mal uso de la plataforma")).thenReturn(inactive);
        when(userService.changeStatus(userId, id, AccountStatus.ACTIVE, "Caso resuelto con el usuario")).thenReturn(active);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/deactivate", id).header(AUTH, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Mal uso de la plataforma\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("INACTIVE"));
        mockMvc.perform(patch("/api/v1/admin/users/{id}/activate", id).header(AUTH, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Caso resuelto con el usuario\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    } // Cierre del método

    @Test
    void deactivateWithoutReasonReturns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/deactivate", UUID.randomUUID()).header(AUTH, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("reason: El motivo es obligatorio")));
        verifyNoInteractions(userService);
    }

    @Test
    void deactivateWithShortReasonReturns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/deactivate", UUID.randomUUID()).header(AUTH, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Corto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reason: El motivo debe tener entre 10 y 500 caracteres"));
        verifyNoInteractions(userService);
    }

    @Test
    void addPhoneWithLettersReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/phones").header(AUTH, token("TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"number\":\"300abc4567\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void addPhoneThatIsNotAColombianCellphoneReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/phones").header(AUTH, token("TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"number\":\"6011234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("number: Ingrese un teléfono válido"));
        verifyNoInteractions(userService);
    }

    /**
     * Prueba que las rutas de teléfonos usan el id que viene en el token.
     */
    @Test
    void phoneEndpointsUseIdFromToken() throws Exception {
        UUID phoneId = UUID.randomUUID();
        when(userService.getOwnPhones(userId)).thenReturn(List.of());
        when(userService.addPhone(eq(userId), any())).thenReturn(List.of());
        when(userService.updatePhone(eq(userId), eq(phoneId), any())).thenReturn(List.of());
        when(userService.removePhone(userId, phoneId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/me/phones").header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/me/phones").header(AUTH, token("TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"number\":\"3109876543\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/users/me/phones/{id}", phoneId).header(AUTH, token("TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"number\":\"3109876543\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/users/me/phones/{id}", phoneId).header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isOk());
    } // Cierre del método

    /**
     * Prueba que la respuesta 401 tiene formato ApiError.
     */
    @Test
    void unauthorizedResponseUsesApiErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Token ausente, inválido o expirado"))
                .andExpect(jsonPath("$.route").value("/api/v1/users/me"));
    } // Cierre del método

    /**
     * Prueba que la respuesta 403 tiene formato ApiError.
     */
    @Test
    void forbiddenResponseUsesApiErrorFormat() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    } // Cierre del método

    /**
     * Prueba que las excepciones de negocio se convierten en ApiError con su código.
     */
    @Test
    void businessExceptionsAreTranslatedToApiError() throws Exception {
        UUID missing = UUID.randomUUID();
        when(userService.getPublicProfile(userId, missing))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado: " + missing));
        when(userService.register(any(UserRequestDTO.class)))
                .thenThrow(new ConflictException("Ya existe una cuenta con ese correo"));

        mockMvc.perform(get("/api/v1/users/{id}", missing).header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado: " + missing));
        mockMvc.perform(post("/api/v1/users").header(AUTH, token("SERVICE")).contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese correo"));
    } // Cierre del método

    @Test
    void malformedInputReturns400WithApiError() throws Exception {
        mockMvc.perform(get("/api/v1/users/no-es-uuid").header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(post("/api/v1/users").header(AUTH, token("SERVICE")).contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.replace("TRABAJADOR", "JEFE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cuerpo de la petición inválido"));
        verifyNoInteractions(userService);
    }

    /**
     * Prueba que la ruta para que el usuario se quite un rol ya no existe (405).
     */
    @Test
    void userCannotRemoveOwnRoleAnymore() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/roles/TRABAJADOR").header(AUTH, token("TRABAJADOR", "CONTRATANTE")))
                .andExpect(status().isMethodNotAllowed());
    } // Cierre del método

    @Test
    void unexpectedErrorsReturn500WithoutDetails() throws Exception {
        when(userService.getOwnProfile(userId)).thenThrow(new IllegalStateException("detalle interno"));

        mockMvc.perform(get("/api/v1/users/me").header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"));
    }
} // Cierre de la clase