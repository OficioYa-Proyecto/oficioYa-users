package edu.eci.dosw.oficioya_users.client;

import edu.eci.dosw.oficioya_users.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Clase que se comunica con el microservicio de Workers.
 * [La usa UserService para pausar el perfil de
 * trabajador al quitar el rol TRABAJADOR y reactivarlo al agregarlo]
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@Slf4j
@Component
public class WorkerClient {
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    private static final String UNAVAILABLE = "El servicio de trabajadores no está disponible. Intenta más tarde";

    private final RestClient restClient;
    private final ServiceTokenProvider tokenProvider;

    /**
     * Constructor que usa Spring para armar el cliente HTTP con la URL de Workers y un tiempo límite
     *
     * @param builder El constructor de RestClient que provee Spring
     * @param url La URL base del microservicio de Worker
     * @param timeoutSeconds Los segundos máximos de conexión y de lectura
     * @param tokenProvider El proveedor del token SERVICE
     */
    @Autowired
    public WorkerClient(RestClient.Builder builder,
                        @Value("${workers.url:http://localhost:8081}") String url,
                        @Value("${http.client.timeout-seconds:15}") long timeoutSeconds,
                        ServiceTokenProvider tokenProvider) {
        this(builder.baseUrl(url).requestFactory(timeoutFactory(Duration.ofSeconds(timeoutSeconds))).build(),
                tokenProvider);
    } // Cierre del constructor

    /**
     * Constructor que recibe el cliente HTTP ya armado para poder probar
     * la clase con un servidor simulado
     *
     * @param restClient El cliente HTTP
     * @param tokenProvider El proveedor del token SERVICE
     */
    WorkerClient(RestClient restClient, ServiceTokenProvider tokenProvider) {
        this.restClient = restClient;
        this.tokenProvider = tokenProvider;
    } // Cierre del constructor

    /**
     * Método que busca el perfil de trabajador de una cuenta en Workers
     *
     * @param userId El id de la cuenta
     * @return Optional<String> El id del perfil en Worker
     */
    public Optional<String> findWorkerIdByUser(UUID userId) {
        Map<?, ?> worker;
        try {
            worker = restClient.get()
                    .uri("/api/v1/workers/by-user/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, tokenProvider.bearer())
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
        if (worker == null || worker.get("id") == null) {
            throw new ServiceUnavailableException(UNAVAILABLE);
        }
        return Optional.of(String.valueOf(worker.get("id")));
    } // Cierre del método

    /**
     * Método que cambia el estado del perfil de trabajador en Workers
     *
     * @param workerId El id del perfil en Worker
     * @param status El nuevo estado (ACTIVE o PAUSED)
     */
    public void updateStatus(String workerId, String status) {
        try {
            restClient.put()
                    .uri("/api/v1/workers/{id}/status", workerId)
                    .header(HttpHeaders.AUTHORIZATION, tokenProvider.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", status))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
        log.info("Perfil de trabajador {} cambiado a {}", workerId, status);
    } // Cierre del método

    /**
     * Método que crea la fábrica de peticiones HTTP con tiempo límite
     *
     * @param timeout El tiempo máximo de conexión y de lectura
     * @return SimpleClientHttpRequestFactory La fábrica configurada para que una petición no se quede
     * esperando indefinidamente
     */
    private static SimpleClientHttpRequestFactory timeoutFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    } // Cierre del método

    /**
     * Método que registra en el log que Workers no respondió
     *
     * @param exception El error de la petición HTTP
     * @return ServiceUnavailableException La excepción 503 para lanzarla con el mismo mensaje en todos los
     *         métodos
     */
    private ServiceUnavailableException unavailable(RestClientException exception) {
        log.warn("Worker no respondió: {}", exception.getMessage());
        return new ServiceUnavailableException(UNAVAILABLE);
    } // Cierre del método
} // Cierre de la clase