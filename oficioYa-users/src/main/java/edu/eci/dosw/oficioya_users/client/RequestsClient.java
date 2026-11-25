package edu.eci.dosw.oficioya_users.client;

import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;


/**
 * Clase que se comunica con el microservicio de Servicios (solicitudes).
 * [La usa UserService para saber si una cuenta tiene solicitudes activas antes de quitarle un rol (RF-13).
 * Cada llamada lleva el token SERVICE de ServiceTokenProvider]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */

@Slf4j
@Component
public class RequestsClient {
    private static final String UNAVAILABLE = "El servicio de solicitudes no está disponible. Intenta más tarde";

    private final RestClient restClient;
    private final ServiceTokenProvider tokenProvider;

    /**
     * Constructor que usa Spring: arma el cliente HTTP con la URL de Servicios y un tiempo límite.
     *
     * @param builder El constructor de RestClient que provee Spring.
     * @param url La URL base del microservicio de Servicios (propiedad requests.url).
     * @param timeoutSeconds Los segundos máximos de conexión y de lectura.
     * @param tokenProvider El proveedor del token SERVICE.
     */
    @Autowired
    public RequestsClient(RestClient.Builder builder,
                          @Value("${requests.url:http://localhost:8081}") String url,
                          @Value("${http.client.timeout-seconds:15}") long timeoutSeconds,
                          ServiceTokenProvider tokenProvider) {
        this(builder.baseUrl(url).requestFactory(timeoutFactory(Duration.ofSeconds(timeoutSeconds))).build(),
                tokenProvider);
    } // Cierre del método

    /**
     * Constructor que recibe el cliente HTTP ya armado, para poder probar la clase con un servidor simulado.
     *
     * @param restClient El cliente HTTP.
     * @param tokenProvider El proveedor del token SERVICE.
     */
    RequestsClient(RestClient restClient, ServiceTokenProvider tokenProvider) {
        this.restClient = restClient;
        this.tokenProvider = tokenProvider;
    } // Cierre del método

    /**
     * Record con el cuerpo que espera Servicios para enviar los correos del RF-13. Servicios arma el asunto y
     * el texto; Users solo decide cuándo avisar y con qué datos. Los campos que no aplican a la acción van en
     * null (Servicios escribe "(no indicado)").
     *
     * @param action La acción: REQUESTED, APPROVED o REJECTED.
     * @param roleRemovalRequestId El id de la petición.
     * @param userId El id del usuario que pidió quitar el rol.
     * @param role El rol que se quiere quitar (TRABAJADOR o CONTRATANTE).
     * @param userName El nombre del usuario.
     * @param reason La razón escrita por el usuario.
     * @param adminReason El motivo del administrador (solo en APPROVED y REJECTED).
     * @param userEmail El correo del usuario, destinatario en APPROVED y REJECTED.
     * @param adminEmail El correo del administrador, destinatario en REQUESTED.
     */
    public record RoleRemovalNotification(String action, UUID roleRemovalRequestId, UUID userId, String role,
                                          String userName, String reason, String adminReason,
                                          String userEmail, String adminEmail) {
    } // Cierre del record

    /**
     * Record que representa la respuesta de Servicios.
     *
     * @param active La cantidad de solicitudes activas.
     */
    record ActiveRequests(Long active) {
    } // Cierre del record

    /**
     * Método que consulta cuántas solicitudes activas (PENDING, ACCEPTED, CONFLICT) tiene una cuenta en un
     * rol.
     *
     * @param userId El id de la cuenta.
     * @param role El rol que se revisa (TRABAJADOR o CONTRATANTE).
     * @return long La cantidad de solicitudes activas, para impedir quitar el rol mientras haya trabajos en
     *         curso. Lanza ServiceUnavailableException (503) si Servicios no responde o responde sin el campo
     *         active.
     */
    public long countActive(UUID userId, RoleName role) {
        ActiveRequests body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/api/v1/requests/active")
                            .queryParam("userId", userId)
                            .queryParam("role", role.name())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, tokenProvider.bearer())
                    .retrieve()
                    .body(ActiveRequests.class);
        } catch (RestClientException exception) {
            log.warn("Servicios no respondió: {}", exception.getMessage());
            throw new ServiceUnavailableException(UNAVAILABLE);
        }
        if (body == null || body.active() == null) {
            log.warn("Servicios respondió sin el campo active");
            throw new ServiceUnavailableException(UNAVAILABLE);
        }
        return body.active();
    } // Cierre del método

    /**
     * Método que le pide a Servicios que envíe el correo de una petición de quitar rol (RF-13). Es de mejor
     * esfuerzo: si Servicios no responde o rechaza la llamada, solo se registra en el log y no se lanza ninguna
     * excepción, para que un fallo del correo no deshaga la decisión del administrador.
     *
     * @param notification Lo que Servicios necesita para armar el correo: la acción (REQUESTED, APPROVED o
     *                     REJECTED), la petición, la cuenta, el rol, las razones y el correo del destinatario.
     */
    public void notifyRoleRemoval(RoleRemovalNotification notification) {
        try {
            restClient.post()
                    .uri("/api/v1/requests/role-removals/notifications")
                    .header(HttpHeaders.AUTHORIZATION, tokenProvider.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("No se pudo avisar a Servicios del correo {} de la petición {}: {}",
                    notification.action(), notification.roleRemovalRequestId(), exception.getMessage());
        }
    } // Cierre del método

    /**
     * Método que crea la fábrica de peticiones HTTP con tiempo límite.
     *
     * @param timeout El tiempo máximo de conexión y de lectura.
     * @return SimpleClientHttpRequestFactory La fábrica configurada, para que una petición no se quede
     *         esperando indefinidamente.
     */
    private static SimpleClientHttpRequestFactory timeoutFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    } // Cierre del método
} // Cierre de la clase