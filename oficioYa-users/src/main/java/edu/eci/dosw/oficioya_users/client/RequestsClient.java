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
     * Constructor que usa Spring: arma el cliente HTTP con la URL de Servicios y un tiempo límite
     *
     * @param builder El constructor de RestClient que provee Spring
     * @param url La URL base del microservicio de Servicios
     * @param timeoutSeconds Los segundos máximos de conexión y de lectura
     * @param tokenProvider El proveedor del token SERVICE
     */
    @Autowired
    public RequestsClient(RestClient.Builder builder,
                        @Value("${requests.url:http://localhost:8081}") String url,
                        @Value("${http.client.timeout-seconds:15}") long timeoutSeconds,
                        ServiceTokenProvider tokenProvider) {
        this(builder.baseUrl(url).requestFactory(timeoutFactory(Duration.ofSeconds(timeoutSeconds))).build(),
                tokenProvider);
    } // Cierre del constructor

    /**
     * Constructor que recibe el cliente HTTP ya armado para poder probar la
     * clase con un servidor simulado
     *
     * @param restClient El cliente HTTP
     * @param tokenProvider El proveedor del token SERVICE
     */
    RequestsClient(RestClient restClient, ServiceTokenProvider tokenProvider) {
        this.restClient = restClient;
        this.tokenProvider = tokenProvider;
    } // Cierre del constructor

    /**
     * Record que representa la respuesta de Servicios
     *
     * @param active La cantidad de solicitudes activas
     */
    record ActiveRequests(Long active) {
    } // Cierre del record

    /**
     * Método que consulta cuántas solicitudes activas (PENDING, ACCEPTED, CONFLICT)
     * tiene una cuenta en un rol
     *
     * @param userId El id de la cuenta
     * @param role El rol que se revisa (CONTRATANTE o TRABAJADOR)
     * @return long La cantidad de solicitudes activas para impedir quitar el rol
     * mientras haya trabajos en curso
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
     * Método que crea la fábrica de peticiones HTTP con tiempo límite
     *
     * @param timeout El tiempo máximo de conexión y de lectura
     * @return SimpleClientHttpRequestFactory La fábrica configurada para que una petición no se quede
     *         esperando indefinidamente
     */
    private static SimpleClientHttpRequestFactory timeoutFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    } // Cierre del método
} // Cierre de la clase