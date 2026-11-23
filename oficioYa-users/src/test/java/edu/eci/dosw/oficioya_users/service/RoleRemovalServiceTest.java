package edu.eci.dosw.oficioya_users.service;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

@ExtendWith(MockitoExtension.class)
class RoleRemovalServiceTest {

    @Mock
    private RoleRemovalRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    private RoleRemovalService service;
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RoleRemovalService(requestRepository, userRepository, userService);
    }

    private static UserEntity user(AccountStatus status, String... roles) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName("Ana Pérez");
        user.setAccountStatus(status);
        Set<RoleEntity> set = new HashSet<>();
        for (String name : roles) {
            RoleEntity role = new RoleEntity();
            role.setName(name);
            set.add(role);
        }
        user.setRoles(set);
        return user;
    }

    private static RoleRemovalRequestDTO body(RoleName role) {
        RoleRemovalRequestDTO dto = new RoleRemovalRequestDTO();
        dto.setRole(role);
        dto.setReason("  Ya no quiero ofrecer mis servicios  ");
        return dto;
    }

    private static RoleRemovalRequestEntity pending(UserEntity user, RoleName role) {
        RoleRemovalRequestEntity request = new RoleRemovalRequestEntity();
        request.setId(UUID.randomUUID());
        request.setUser(user);
        request.setRole(role);
        request.setReason("Ya no quiero ofrecer mis servicios");
        request.setStatus(RequestStatus.PENDING);
        return request;
    }

    private static void assertStatus(Executable action, HttpStatus status) {
        assertEquals(status, assertThrows(ApiException.class, action).getStatus());
    }

    private void saveReturnsArgument() {
        when(requestRepository.save(any(RoleRemovalRequestEntity.class))).thenAnswer(i -> i.getArgument(0));
    }

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
    }

    @Test
    void createRejectsAdminRole() {
        assertStatus(() -> service.create(UUID.randomUUID(), body(RoleName.ADMIN)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository, requestRepository);
    }

    @Test
    void createRejectsInactiveAccount() {
        UserEntity user = user(AccountStatus.INACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.FORBIDDEN);
    }

    @Test
    void createRejectsAdminAccount() {
        UserEntity user = user(AccountStatus.ACTIVE, "ADMIN");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.FORBIDDEN);
    }

    @Test
    void createRejectsMissingRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.CONFLICT);
    }

    @Test
    void createRejectsLastRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> service.create(user.getId(), body(RoleName.TRABAJADOR)), HttpStatus.CONFLICT);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void createRejectsWhenAnotherRequestIsPending() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(requestRepository.existsByUserIdAndStatus(user.getId(), RequestStatus.PENDING)).thenReturn(true);

        assertStatus(() -> service.create(user.getId(), body(RoleName.CONTRATANTE)), HttpStatus.CONFLICT);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void createReturns404WhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> service.create(id, body(RoleName.TRABAJADOR)), HttpStatus.NOT_FOUND);
    }

    @Test
    void findOwnReturnsUserRequests() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(requestRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(pending(user, RoleName.TRABAJADOR)));

        assertThat(service.findOwn(user.getId())).hasSize(1);
    }

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
    }

    @Test
    void findAllByStatusOrWithoutFilters() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(requestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING))
                .thenReturn(List.of(pending(user, RoleName.TRABAJADOR)));
        when(requestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(service.findAll(RequestStatus.PENDING, null)).hasSize(1);
        assertThat(service.findAll(null, null)).isEmpty();
    }

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
    }

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
    }

    @Test
    void rejectResolvesRequestWithoutRemovingRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.CONTRATANTE);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        RoleRemovalResponseDTO result = service.reject(adminId, request.getId(), "Tiene servicios por cerrar");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.getAdminReason()).isEqualTo("Tiene servicios por cerrar");
        verifyNoInteractions(userService);
    }

    @Test
    void resolvingTwiceReturnsConflict() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        RoleRemovalRequestEntity request = pending(user, RoleName.CONTRATANTE);
        request.setStatus(RequestStatus.APPROVED);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertStatus(() -> service.reject(adminId, request.getId(), "Motivo cualquiera"), HttpStatus.CONFLICT);
    }

    @Test
    void resolvingUnknownRequestReturns404() {
        UUID id = UUID.randomUUID();
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> service.approve(adminId, id, "Motivo cualquiera"), HttpStatus.NOT_FOUND);
    }

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
    }

    @Test
    void removeByAdminWithoutRequestOnlyRemovesRole() {
        UUID userId = UUID.randomUUID();
        when(requestRepository.findFirstByUserIdAndRoleAndStatus(userId, RoleName.CONTRATANTE, RequestStatus.PENDING))
                .thenReturn(Optional.empty());

        service.removeByAdmin(adminId, userId, RoleName.CONTRATANTE);

        verify(userService).removeRole(adminId, userId, RoleName.CONTRATANTE);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void removeByAdminDoesNotResolveRequestWhenRemovalFails() {
        UUID userId = UUID.randomUUID();
        when(userService.removeRole(adminId, userId, RoleName.TRABAJADOR))
                .thenThrow(new ConflictException("La cuenta tiene 1 solicitudes activas"));

        assertStatus(() -> service.removeByAdmin(adminId, userId, RoleName.TRABAJADOR), HttpStatus.CONFLICT);
        verify(requestRepository, never()).findFirstByUserIdAndRoleAndStatus(any(), any(), any());
    }
}