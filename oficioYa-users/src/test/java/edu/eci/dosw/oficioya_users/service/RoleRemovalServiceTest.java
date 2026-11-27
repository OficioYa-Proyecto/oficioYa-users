package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.client.RequestsClient;
import edu.eci.dosw.oficioya_users.client.RequestsClient.RoleRemovalNotification;
import edu.eci.dosw.oficioya_users.dto.RoleRemovalRequestDTO;
import edu.eci.dosw.oficioya_users.dto.RoleRemovalResponseDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.entity.RoleEntity;
import edu.eci.dosw.oficioya_users.entity.RoleRemovalRequestEntity;
import edu.eci.dosw.oficioya_users.entity.UserEntity;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import edu.eci.dosw.oficioya_users.enums.RequestStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.exception.ApiException;
import edu.eci.dosw.oficioya_users.exception.ConflictException;
import edu.eci.dosw.oficioya_users.repository.RoleRemovalRequestRepository;
import edu.eci.dosw.oficioya_users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas de RoleRemovalService (RF-13) con los repositorios, UserService y RequestsClient simulados
 * (Mockito), incluido el payload de los correos que se piden a Servicios.
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@ExtendWith(MockitoExtension.class)
class RoleRemovalServiceTest {

    @Mock
    private RoleRemovalRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private RequestsClient requestsClient;

    private static final String ADMIN_NOTIFICATION_EMAIL = "admin@oficioya.com";
    private static final String USER_EMAIL = "ana@mail.com";

    private RoleRemovalService service;
    private final UUID adminId = UUID.randomUUID();

    /**
     * Método que se ejecuta antes de cada prueba y prepara el servicio con sus dependencias simuladas.
     */
    @BeforeEach
    void setUp() {
        service = new RoleRemovalService(requestRepository, userRepository, userService, requestsClient, ADMIN_NOTIFICATION_EMAIL);
    } // Cierre del método

    /**
     * Método que crea una cuenta de prueba.
     *
     * @param status El estado de la cuenta.
     * @param roles Los roles de la cuenta.
     * @return UserEntity La cuenta.
     */
    private static UserEntity user(AccountStatus status, String... roles) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName("Ana Pérez");
        user.setEmail(USER_EMAIL);
        user.setAccountStatus(status);
        Set<RoleEntity> set = new HashSet<>();
        for (String name : roles) {
            RoleEntity role = new RoleEntity();
            role.setName(name);
            set.add(role);
        }
        user.setRoles(set);
        return user;
    } // Cierre del método

    /**
     * Método que crea el cuerpo de una petición con una razón válida.
     *
     * @param role El rol a quitar.
     * @return RoleRemovalRequestDTO El cuerpo de la petición.
     */
    private static RoleRemovalRequestDTO body(RoleName role) {
        RoleRemovalRequestDTO dto = new RoleRemovalRequestDTO();
        dto.setRole(role);
        dto.setReason("  Ya no quiero ofrecer mis servicios  ");
        return dto;
    } // Cierre del método

    /**
     * Método que crea una petición pendiente.
     *
     * @param user La cuenta.
     * @param role El rol.
     * @return RoleRemovalRequestEntity La petición en PENDING.
     */
    private static RoleRemovalRequestEntity pending(UserEntity user, RoleName role) {
        RoleRemovalRequestEntity request = new RoleRemovalRequestEntity();
        request.setId(UUID.randomUUID());
        request.setUser(user);
        request.setRole(role);
        request.setReason("Ya no quiero ofrecer mis servicios");
        request.setStatus(RequestStatus.PENDING);
        return request;
    } // Cierre del método

    /**
     * Método que verifica que una acción lance una ApiException con un código HTTP.
     *
     * @param action La acción que debe fallar.
     * @param status El código esperado.
     */
    private static void assertStatus(Executable action, HttpStatus status) {
        assertEquals(status, assertThrows(ApiException.class, action).getStatus());
    } // Cierre del método

    /**
     * Método que captura el aviso que se le pidió enviar a Servicios.
     *
     * @return RoleRemovalNotification El aviso, para revisar su contenido.
     */
    private RoleRemovalNotification sentNotification() {
        ArgumentCaptor<RoleRemovalNotification> captor = ArgumentCaptor.forClass(RoleRemovalNotification.class);
        verify(requestsClient).notifyRoleRemoval(captor.capture());
        return captor.getValue();
    } // Cierre del método

    /**
     * Método que simula que guardar una petición devuelve la misma petición.
     */
    private void saveReturnsArgument() {
        when(requestRepository.save(any(RoleRemovalRequestEntity.class))).thenAnswer(i -> i.getArgument(0));
    } // Cierre del método

    /**
     * Prueba que se guarda la petición en PENDING con la razón sin espacios sobrantes.
     */
    @Test
    void createSavesPendingRequestWithTrimmedReason() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        RoleRemovalResponseDTO result = service.create(user.getId(), body(RoleName.TRABAJADOR));

        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getRole()).isEqualTo(RoleName.TRABAJADOR);
        assertThat(result.getReason()).isEqualTo("Ya no quiero ofrecer mis servicios");
        assertThat(result.getUserName()).isEqualTo("Ana Pérez");
        verifyNoInteractions(userService);
    } // Cierre del método

    /**
     * Prueba que no se puede pedir quitar el rol ADMIN (400).
     */
    @Test
    void createRejectsAdminRole() {
        assertStatus(() -> service.create(UUID.randomUUID(), body(RoleName.ADMIN)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository, requestRepository);
    } // Cierre del método

    /**
     * Prueba que una cuenta inactiva no puede crear peticiones (403).
     */
    @Test
    void createRejectsInactiveAccount() {
        UserEntity user = user(AccountStatus.INACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.FORBIDDEN);
    } // Cierre del método

    /**
     * Prueba que el administrador no puede crear peticiones (403).
     */
    @Test
    void createRejectsAdminAccount() {
        UserEntity user = user(AccountStatus.ACTIVE, "ADMIN");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.FORBIDDEN);
    } // Cierre del método

    /**
     * Prueba que no se puede pedir quitar un rol que la cuenta no tiene (409).
     */
    @Test
    void createRejectsMissingRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.CONFLICT);
    } // Cierre del método

    /**
     * Prueba que no se puede pedir quitar el único rol de la cuenta (409).
     */
    @Test
    void createRejectsLastRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.CONFLICT);
        verify(requestRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que no se puede crear una petición si ya hay otra pendiente (409).
     */
    @Test
    void createRejectsWhenAnotherRequestIsPending() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(requestRepository.existsByUserIdAndStatus(user.getId(), RequestStatus.PENDING)).thenReturn(true);

        assertStatus(() -> service.create(user.getId(), body(RoleName.CONTRATANTE)), HttpStatus.CONFLICT);
        verify(requestRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que crear una petición falla si la cuenta no existe (404).
     */
    @Test
    void createReturns404WhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> service.create(id, body(RoleName.TRABAJADOR)), HttpStatus.NOT_FOUND);
    } // Cierre del método

    /**
     * Prueba que se devuelven las peticiones del usuario.
     */
    @Test
    void findOwnReturnsUserRequests() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(requestRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(pending(user, RoleName.TRABAJADOR)));

        assertThat(service.findOwn(user.getId())).hasSize(1);
    } // Cierre del método

    /**
     * Prueba que se filtran las peticiones por usuario y estado.
     */
    @Test
    void findAllFiltersByUserAndStatus() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity rejected = pending(user, RoleName.CONTRATANTE);
        rejected.setStatus(RequestStatus.REJECTED);
        when(requestRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(pending(user, RoleName.TRABAJADOR), rejected));

        assertThat(service.findAll(RequestStatus.PENDING, user.getId()))
                .extracting(RoleRemovalResponseDTO::getRole).containsExactly(RoleName.TRABAJADOR);
        assertThat(service.findAll(null, user.getId())).hasSize(2);
    } // Cierre del método

    /**
     * Prueba que se filtran por estado o se devuelven todas sin filtros.
     */
    @Test
    void findAllByStatusOrWithoutFilters() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(requestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING))
                .thenReturn(List.of(pending(user, RoleName.TRABAJADOR)));
        when(requestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(service.findAll(RequestStatus.PENDING, null)).hasSize(1);
        assertThat(service.findAll(null, null)).isEmpty();
    } // Cierre del método

    /**
     * Prueba que aprobar quita el rol y marca la petición como APPROVED.
     */
    @Test
    void approveRemovesRoleAndResolvesRequest() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.TRABAJADOR);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        RoleRemovalResponseDTO result = service.approve(adminId, request.getId(), " Petición válida ");

        verify(userService).removeRole(adminId, user.getId(), RoleName.TRABAJADOR);
        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.getAdminReason()).isEqualTo("Petición válida");
        assertThat(result.getResolvedAt()).isNotNull();
    } // Cierre del método

    /**
     * Prueba que si no se puede quitar el rol la petición sigue pendiente.
     */
    @Test
    void approveKeepsRequestPendingWhenRoleCannotBeRemoved() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.TRABAJADOR);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR))
                .thenThrow(new ConflictException("solicitudes activas"));

        assertStatus(() -> service.approve(adminId, request.getId(), "Petición válida"), HttpStatus.CONFLICT);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(requestRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que rechazar marca la petición como REJECTED sin quitar el rol.
     */
    @Test
    void rejectResolvesRequestWithoutRemovingRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.CONTRATANTE);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        RoleRemovalResponseDTO result = service.reject(adminId, request.getId(), "Tiene servicios por cerrar");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.getAdminReason()).isEqualTo("Tiene servicios por cerrar");
        verifyNoInteractions(userService);
    } // Cierre del método

    /**
     * Prueba que no se puede resolver dos veces la misma petición (409).
     */
    @Test
    void resolvingTwiceReturnsConflict() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.CONTRATANTE);
        request.setStatus(RequestStatus.APPROVED);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertStatus(() -> service.reject(adminId, request.getId(), "Motivo cualquiera"), HttpStatus.CONFLICT);
    } // Cierre del método

    /**
     * Prueba que resolver una petición que no existe falla (404).
     */
    @Test
    void resolvingUnknownRequestReturns404() {
        UUID id = UUID.randomUUID();
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> service.approve(adminId, id, "Motivo cualquiera"), HttpStatus.NOT_FOUND);
    } // Cierre del método

    /**
     * Prueba que al quitar el rol directamente se aprueba la petición pendiente de ese rol.
     */
    @Test
    void removeByAdminApprovesPendingRequestForThatRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.TRABAJADOR);
        UserResponseDTO updated = new UserResponseDTO();
        when(userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR)).thenReturn(updated);
        when(requestRepository.findFirstByUserIdAndRoleAndStatus(user.getId(), RoleName.TRABAJADOR, RequestStatus.PENDING))
                .thenReturn(Optional.of(request));

        assertThat(service.removeByAdmin(adminId, user.getId(), RoleName.TRABAJADOR)).isSameAs(updated);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getAdminReason()).isEqualTo(RoleRemovalService.DIRECT_REMOVAL_REASON);
        assertThat(request.getResolvedAt()).isNotNull();
    } // Cierre del método

    /**
     * Prueba que sin petición pendiente solo se quita el rol.
     */
    @Test
    void removeByAdminWithoutRequestOnlyRemovesRole() {
        UUID userId = UUID.randomUUID();
        when(requestRepository.findFirstByUserIdAndRoleAndStatus(userId, RoleName.CONTRATANTE, RequestStatus.PENDING))
                .thenReturn(Optional.empty());

        service.removeByAdmin(adminId, userId, RoleName.CONTRATANTE);

        verify(userService).removeRole(adminId, userId, RoleName.CONTRATANTE);
        verify(requestRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que si quitar el rol falla la petición no se resuelve.
     */
    @Test
    void removeByAdminDoesNotResolveRequestWhenRemovalFails() {
        UUID userId = UUID.randomUUID();
        when(userService.removeRole(adminId, userId, RoleName.TRABAJADOR))
                .thenThrow(new ConflictException("La cuenta tiene 1 solicitudes activas"));

        assertStatus(() -> service.removeByAdmin(adminId, userId, RoleName.TRABAJADOR), HttpStatus.CONFLICT);
        verify(requestRepository, never()).findFirstByUserIdAndRoleAndStatus(any(), any(), any());
    } // Cierre del método

    /**
     * Prueba que al crear la petición se avisa al administrador con el payload REQUESTED (solo con su correo).
     */
    @Test
    void createNotifiesAdminWithRequestedPayload() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        service.create(user.getId(), body(RoleName.TRABAJADOR));

        RoleRemovalNotification sent = sentNotification();
        assertThat(sent.action()).isEqualTo("REQUESTED");
        assertThat(sent.userId()).isEqualTo(user.getId());
        assertThat(sent.role()).isEqualTo("TRABAJADOR");
        assertThat(sent.userName()).isEqualTo("Ana Pérez");
        assertThat(sent.reason()).isEqualTo("Ya no quiero ofrecer mis servicios");
        assertThat(sent.adminReason()).isNull();
        assertThat(sent.adminEmail()).isEqualTo(ADMIN_NOTIFICATION_EMAIL);
        assertThat(sent.userEmail()).isNull();
    } // Cierre del método

    /**
     * Prueba que no se avisa a nadie si la petición no se puede crear.
     */
    @Test
    void createDoesNotNotifyWhenRequestIsRejected() {
        UserEntity user = user(AccountStatus.INACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.FORBIDDEN);
        verifyNoInteractions(requestsClient);
    } // Cierre del método

    /**
     * Prueba que si falta ADMIN_NOTIFICATION_EMAIL la petición se crea igual pero no se llama a Servicios.
     */
    @Test
    void createWithoutAdminEmailSavesRequestButDoesNotNotify() {
        service = new RoleRemovalService(requestRepository, userRepository, userService, requestsClient, " ");
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        RoleRemovalResponseDTO result = service.create(user.getId(), body(RoleName.TRABAJADOR));

        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        verifyNoInteractions(requestsClient);
    } // Cierre del método

    /**
     * Prueba que el aviso se envía solo después de confirmar la transacción.
     */
    @Test
    void notificationIsSentOnlyAfterCommit() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.create(user.getId(), body(RoleName.TRABAJADOR));
            verifyNoInteractions(requestsClient);

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(requestsClient).notifyRoleRemoval(any(RoleRemovalNotification.class));
        } finally {
            TransactionSynchronizationManager.clear();
        }
    } // Cierre del método

    /**
     * Prueba que al aprobar se avisa al usuario con el payload APPROVED, su correo y el motivo del administrador.
     */
    @Test
    void approveNotifiesUserWithApprovedPayload() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.TRABAJADOR);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        service.approve(adminId, request.getId(), " Petición válida ");

        RoleRemovalNotification sent = sentNotification();
        assertThat(sent.action()).isEqualTo("APPROVED");
        assertThat(sent.roleRemovalRequestId()).isEqualTo(request.getId());
        assertThat(sent.userId()).isEqualTo(user.getId());
        assertThat(sent.role()).isEqualTo("TRABAJADOR");
        assertThat(sent.reason()).isEqualTo("Ya no quiero ofrecer mis servicios");
        assertThat(sent.adminReason()).isEqualTo("Petición válida");
        assertThat(sent.userEmail()).isEqualTo(USER_EMAIL);
        assertThat(sent.adminEmail()).isNull();
    } // Cierre del método

    /**
     * Prueba que si no se puede quitar el rol al aprobar no se avisa a nadie.
     */
    @Test
    void approveDoesNotNotifyWhenRoleCannotBeRemoved() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.TRABAJADOR);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR))
                .thenThrow(new ConflictException("solicitudes activas"));

        assertStatus(() -> service.approve(adminId, request.getId(), "Petición válida"), HttpStatus.CONFLICT);
        verifyNoInteractions(requestsClient);
    } // Cierre del método

    /**
     * Prueba que al rechazar se avisa al usuario con el payload REJECTED, su correo y el motivo del administrador.
     */
    @Test
    void rejectNotifiesUserWithRejectedPayload() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.CONTRATANTE);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        service.reject(adminId, request.getId(), "Tiene servicios por cerrar");

        RoleRemovalNotification sent = sentNotification();
        assertThat(sent.action()).isEqualTo("REJECTED");
        assertThat(sent.roleRemovalRequestId()).isEqualTo(request.getId());
        assertThat(sent.role()).isEqualTo("CONTRATANTE");
        assertThat(sent.adminReason()).isEqualTo("Tiene servicios por cerrar");
        assertThat(sent.userEmail()).isEqualTo(USER_EMAIL);
        assertThat(sent.adminEmail()).isNull();
    } // Cierre del método

    /**
     * Prueba que al quitar el rol directamente con una petición pendiente se avisa al usuario con APPROVED y el motivo fijo.
     */
    @Test
    void removeByAdminNotifiesApprovedWhenThereWasPendingRequest() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.TRABAJADOR);
        when(requestRepository.findFirstByUserIdAndRoleAndStatus(user.getId(), RoleName.TRABAJADOR, RequestStatus.PENDING))
                .thenReturn(Optional.of(request));

        service.removeByAdmin(adminId, user.getId(), RoleName.TRABAJADOR);

        RoleRemovalNotification sent = sentNotification();
        assertThat(sent.action()).isEqualTo("APPROVED");
        assertThat(sent.adminReason()).isEqualTo(RoleRemovalService.DIRECT_REMOVAL_REASON);
        assertThat(sent.userEmail()).isEqualTo(USER_EMAIL);
    } // Cierre del método

    /**
     * Prueba que al quitar el rol directamente sin petición pendiente no se envía ningún correo.
     */
    @Test
    void removeByAdminWithoutRequestDoesNotNotify() {
        UUID userId = UUID.randomUUID();
        when(requestRepository.findFirstByUserIdAndRoleAndStatus(userId, RoleName.CONTRATANTE, RequestStatus.PENDING))
                .thenReturn(Optional.empty());

        service.removeByAdmin(adminId, userId, RoleName.CONTRATANTE);

        verifyNoInteractions(requestsClient);
    } // Cierre del método
} // Cierre de la clase