package edu.eci.dosw.oficioya_users.client;

import edu.eci.dosw.oficioya_users.client.RequestsClient.RoleRemovalNotification;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Clase de pruebas de RequestsClient con un servidor HTTP simulado (MockRestServiceServer)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
class RequestsClientTest {

    private static final String SECRET = "IadhiSp/AVyloXuE15l29oZzpucys4NEfqpHn6PEMeQ=";

    private final UUID userId = UUID.randomUUID();
    private MockRestServiceServer server;
    private RequestsClient client;

    /**
     * Método que se ejecuta antes de cada prueba y prepara el servidor simulado y el cliente con un
     * ServiceTokenProvider real.
     */
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://requests");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RequestsClient(builder.build(), new ServiceTokenProvider(SECRET, "roles"));
    } // Cierre del método

    /**
     * Método que arma la URL esperada de la consulta de solicitudes activas.
     *
     * @param role El rol consultado.
     * @return String La URL con userId y role.
     */
    private String activeUrl(RoleName role) {
        return "http://requests/api/v1/requests/active?userId=" + userId + "&role=" + role;
    } // Cierre del método

    /**
     * Prueba que se envía el token SERVICE y se devuelve la cantidad de solicitudes activas.
     */
    @Test
    void countActiveReturnsValueUsingServiceToken() {
        server.expect(requestTo(activeUrl(RoleName.TRABAJADOR)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
                .andRespond(withSuccess("{\"active\":2}", MediaType.APPLICATION_JSON));

        assertThat(client.countActive(userId, RoleName.TRABAJADOR)).isEqualTo(2);
        server.verify();
    } // Cierre del método

    /**
     * Prueba que se devuelve 0 cuando no hay solicitudes activas.
     */
    @Test
    void countActiveReturnsZeroWhenNoActiveRequests() {
        server.expect(requestTo(activeUrl(RoleName.CONTRATANTE)))
                .andRespond(withSuccess("{\"active\":0}", MediaType.APPLICATION_JSON));

        assertThat(client.countActive(userId, RoleName.CONTRATANTE)).isZero();
    } // Cierre del método

    /**
     * Prueba que se lanza 503 si Servicios responde con error.
     */
    @Test
    void countActiveThrows503WhenServiceFails() {
        server.expect(requestTo(activeUrl(RoleName.TRABAJADOR))).andRespond(withServerError());

        assertThrows(ServiceUnavailableException.class, () -> client.countActive(userId, RoleName.TRABAJADOR));
    } // Cierre del método

    /**
     * Prueba que se lanza 503 si la respuesta de Servicios no trae el campo active.
     */
    @Test
    void countActiveThrows503WhenBodyHasNoActive() {
        server.expect(requestTo(activeUrl(RoleName.TRABAJADOR)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(ServiceUnavailableException.class, () -> client.countActive(userId, RoleName.TRABAJADOR));
    } // Cierre del método

    /**
     * Prueba que se lanza 503 si el campo active no es un número.
     */
    @Test
    void countActiveThrows503WhenActiveIsNotNumber() {
        server.expect(requestTo(activeUrl(RoleName.TRABAJADOR)))
                .andRespond(withSuccess("{\"active\":\"dos\"}", MediaType.APPLICATION_JSON));

        assertThrows(ServiceUnavailableException.class, () -> client.countActive(userId, RoleName.TRABAJADOR));
    } // Cierre del método

    /**
     * Prueba que el aviso se envía por POST con el token SERVICE y con el cuerpo que espera Servicios.
     */
    @Test
    void notifyRoleRemovalPostsPayloadUsingServiceToken() {
        UUID requestId = UUID.randomUUID();
        server.expect(requestTo("http://requests/api/v1/requests/role-removals/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.action").value("APPROVED"))
                .andExpect(jsonPath("$.roleRemovalRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("TRABAJADOR"))
                .andExpect(jsonPath("$.userName").value("Ana Pérez"))
                .andExpect(jsonPath("$.reason").value("Ya no quiero ofrecer mis servicios"))
                .andExpect(jsonPath("$.adminReason").value("Petición válida"))
                .andExpect(jsonPath("$.userEmail").value("ana@mail.com"))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        client.notifyRoleRemoval(new RoleRemovalNotification("APPROVED", requestId, userId, "TRABAJADOR",
                "Ana Pérez", "Ya no quiero ofrecer mis servicios", "Petición válida", "ana@mail.com", null));

        server.verify();
    } // Cierre del método

    /**
     * Prueba que si Servicios falla no se lanza ninguna excepción, para no deshacer la decisión del
     * administrador.
     */
    @Test
    void notifyRoleRemovalDoesNotThrowWhenServiceFails() {
        server.expect(requestTo("http://requests/api/v1/requests/role-removals/notifications"))
                .andRespond(withServerError());

        assertDoesNotThrow(() -> client.notifyRoleRemoval(new RoleRemovalNotification("REQUESTED", null, userId,
                "CONTRATANTE", "Ana Pérez", "Ya no quiero contratar", null, null, "admin@oficioya.com")));
        server.verify();
    } // Cierre del método
} // Cierre de la clase