package edu.eci.dosw.oficioya_users.client;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Clase de pruebas de WorkerClient con un servidor HTTP simulado (MockRestServiceServer)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
class WorkerClientTest {

    private static final String SECRET = "IadhiSp/AVyloXuE15l29oZzpucys4NEfqpHn6PEMeQ=";
    private static final String BY_USER = "http://worker/api/v1/workers/by-user/";

    private final UUID userId = UUID.randomUUID();
    private MockRestServiceServer server;
    private WorkerClient client;

    /**
     * Método que se ejecuta antes de cada prueba y prepara el servidor simulado y el cliente con un
     * ServiceTokenProvider real
     */
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://worker");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new WorkerClient(builder.build(), new ServiceTokenProvider(SECRET, "roles"));
    } // Cierre del método

    /**
     * Prueba que se envía el token SERVICE y se devuelve el id del perfil de trabajador
     */
    @Test
    void findWorkerIdByUserReturnsIdUsingServiceToken() {
        server.expect(requestTo(BY_USER + userId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, startsWith("Bearer ")))
                .andRespond(withSuccess("{\"id\":\"w-1\"}", MediaType.APPLICATION_JSON));

        assertThat(client.findWorkerIdByUser(userId)).contains("w-1");
    } // Cierre del método

    @Test
    void findWorkerIdByUserReturnsEmptyOn404() {
        server.expect(requestTo(BY_USER + userId)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findWorkerIdByUser(userId)).isEmpty();
    }

    @Test
    void findWorkerIdByUserThrows503WhenWorkerFails() {
        server.expect(requestTo(BY_USER + userId)).andRespond(withServerError());

        assertThrows(ServiceUnavailableException.class, () -> client.findWorkerIdByUser(userId));
    }

    /**
     * Prueba que se envía el nuevo estado del perfil a Worker
     */
    @Test
    void updateStatusSendsNewStatus() {
        server.expect(requestTo("http://worker/api/v1/workers/w-1/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"status\":\"PAUSED\"}"))
                .andRespond(withSuccess());

        client.updateStatus("w-1", WorkerClient.PAUSED);
        server.verify();
    } // Cierre del método
} // Cierre de la clase