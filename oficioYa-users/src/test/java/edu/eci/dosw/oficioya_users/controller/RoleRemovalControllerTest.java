package edu.eci.dosw.oficioya_users.controller;

import edu.eci.dosw.oficioya_users.dto.RoleRemovalRequestDTO;
import edu.eci.dosw.oficioya_users.dto.RoleRemovalResponseDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.exception.ConflictException;
import edu.eci.dosw.oficioya_users.exception.ResourceNotFoundException;
import edu.eci.dosw.oficioya_users.security.SecurityConfig;
import edu.eci.dosw.oficioya_users.service.RoleRemovalService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Clase de pruebas de RoleRemovalController (RF-13) con la seguridad real y el servicio simulado
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@WebMvcTest(RoleRemovalController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=" + RoleRemovalControllerTest.SECRET)
class RoleRemovalControllerTest {
    static final String SECRET = "IadhiSp/AVyloXuE15l29oZzpucys4NEfqpHn6PEMeQ=";
    private static final String AUTH = "Authorization";
    private static final String OWN = "/api/v1/users/me/role-removal-requests";
    private static final String ADMIN = "/api/v1/admin/role-removal-requests";
    private static final String REASON_BODY = "{\"reason\":\"Motivo de prueba válido\"}";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoleRemovalService roleRemovalService;

    private final UUID userId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    /**
     * Método que firma un token de prueba para el usuario de la prueba.
     *
     * @param roles Los roles del token.
     * @return String El encabezado "Bearer <token>".
     */
    private String token(String... roles) {
        return "Bearer " + Jwts.builder()
                .subject(userId.toString())
                .claim("roles", List.of(roles))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    } // Cierre del método

    /**
     * Método que crea una petición de respuesta de ejemplo.
     *
     * @param status El estado de la petición.
     * @return RoleRemovalResponseDTO La petición, para simular el servicio.
     */
    private static RoleRemovalResponseDTO response(RequestStatus status) {
        RoleRemovalResponseDTO dto = new RoleRemovalResponseDTO();
        dto.setRole(RoleName.TRABAJADOR);
        dto.setStatus(status);
        return dto;
    } // Cierre del método

    /**
     * Prueba que el usuario crea una petición y recibe 201.
     */
    @Test
    void userCreatesRequest() throws Exception {
        when(roleRemovalService.create(eq(userId), any(RoleRemovalRequestDTO.class)))
                .thenReturn(response(RequestStatus.PENDING));

        mockMvc.perform(post(OWN).header(AUTH, token("TRABAJADOR", "CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TRABAJADOR\",\"reason\":\"Ya no quiero trabajar aquí\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    } // Cierre del método

    /**
     * Prueba que crear una petición exige la razón (400).
     */
    @Test
    void createRequiresReason() throws Exception {
        mockMvc.perform(post(OWN).header(AUTH, token("TRABAJADOR", "CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TRABAJADOR\",\"reason\":\"corta\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(roleRemovalService);
    } // Cierre del método

    /**
     * Prueba que un conflicto del servicio se responde como 409.
     */
    @Test
    void createReturnsConflictFromService() throws Exception {
        when(roleRemovalService.create(eq(userId), any(RoleRemovalRequestDTO.class)))
                .thenThrow(new ConflictException("Ya tienes una petición pendiente"));

        mockMvc.perform(post(OWN).header(AUTH, token("TRABAJADOR", "CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CONTRATANTE\",\"reason\":\"Ya no quiero contratar\"}"))
                .andExpect(status().isConflict());
    } // Cierre del método

    /**
     * Prueba que el usuario lista sus peticiones.
     */
    @Test
    void userListsOwnRequests() throws Exception {
        when(roleRemovalService.findOwn(userId)).thenReturn(List.of(response(RequestStatus.REJECTED)));

        mockMvc.perform(get(OWN).header(AUTH, token("CONTRATANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"));
    } // Cierre del método

    /**
     * Prueba que las rutas de administración exigen el rol ADMIN (403).
     */
    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get(ADMIN).header(AUTH, token("TRABAJADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(ADMIN + "/" + requestId + "/approve").header(AUTH, token("TRABAJADOR", "CONTRATANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(REASON_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(roleRemovalService);
    } // Cierre del método

    /**
     * Prueba que el administrador lista las peticiones pendientes.
     */
    @Test
    void adminListsPendingRequests() throws Exception {
        when(roleRemovalService.findAll(RequestStatus.PENDING, null)).thenReturn(List.of(response(RequestStatus.PENDING)));

        mockMvc.perform(get(ADMIN).param("status", "PENDING").header(AUTH, token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("TRABAJADOR"));
    } // Cierre del método

    /**
     * Prueba que el administrador aprueba una petición con motivo.
     */
    @Test
    void adminApprovesRequest() throws Exception {
        when(roleRemovalService.approve(userId, requestId, "Motivo de prueba válido"))
                .thenReturn(response(RequestStatus.APPROVED));

        mockMvc.perform(patch(ADMIN + "/" + requestId + "/approve").header(AUTH, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(REASON_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    } // Cierre del método

    /**
     * Prueba que el administrador rechaza una petición con motivo.
     */
    @Test
    void adminRejectsRequest() throws Exception {
        when(roleRemovalService.reject(userId, requestId, "Motivo de prueba válido"))
                .thenReturn(response(RequestStatus.REJECTED));

        mockMvc.perform(patch(ADMIN + "/" + requestId + "/reject").header(AUTH, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(REASON_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    } // Cierre del método

    /**
     * Prueba que rechazar exige un motivo (400).
     */
    @Test
    void rejectRequiresReason() throws Exception {
        mockMvc.perform(patch(ADMIN + "/" + requestId + "/reject").header(AUTH, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(roleRemovalService);
    } // Cierre del método

    /**
     * Prueba que el administrador quita un rol directamente sin body.
     */
    @Test
    void adminRemovesRoleDirectly() throws Exception {
        UUID target = UUID.randomUUID();
        UserResponseDTO user = new UserResponseDTO();
        user.setName("Ana");
        when(roleRemovalService.removeByAdmin(userId, target, RoleName.CONTRATANTE)).thenReturn(user);

        mockMvc.perform(delete("/api/v1/admin/users/" + target + "/roles/CONTRATANTE").header(AUTH, token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"));
    } // Cierre del método

    /**
     * Prueba que un usuario que no es administrador no puede quitar roles (403).
     */
    @Test
    void nonAdminCannotRemoveRoles() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + userId + "/roles/TRABAJADOR")
                        .header(AUTH, token("TRABAJADOR", "CONTRATANTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(roleRemovalService);
    } // Cierre del método

    /**
     * Prueba que un error del servicio al quitar un rol (cuenta no encontrada) se responde con su código
     * (404).
     */
    @Test
    void removeRoleReturnsServiceErrors() throws Exception {
        UUID target = UUID.randomUUID();
        when(roleRemovalService.removeByAdmin(userId, target, RoleName.TRABAJADOR))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado: " + target));

        mockMvc.perform(delete("/api/v1/admin/users/" + target + "/roles/TRABAJADOR").header(AUTH, token("ADMIN")))
                .andExpect(status().isNotFound());
    } // Cierre del método

    /**
     * Prueba que la ruta para que el usuario se quite un rol ya no existe (404).
     */
    @Test
    void userCannotRemoveOwnRoleAnymore() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/roles/TRABAJADOR").header(AUTH, token("TRABAJADOR", "CONTRATANTE")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(roleRemovalService);
    } // Cierre del método
} // Cierre de la clase
