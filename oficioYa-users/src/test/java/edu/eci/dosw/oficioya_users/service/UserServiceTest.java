package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.client.WorkerClient;
import edu.eci.dosw.oficioya_users.dto.AccountStatsDTO;
import edu.eci.dosw.oficioya_users.client.RequestsClient;
import edu.eci.dosw.oficioya_users.dto.PhoneDTO;
import edu.eci.dosw.oficioya_users.dto.RoleCountDTO;
import edu.eci.dosw.oficioya_users.dto.UserRequestDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.dto.UserUpdateDTO;
import edu.eci.dosw.oficioya_users.entity.LocalityEntity;
import edu.eci.dosw.oficioya_users.entity.PhoneEntity;
import edu.eci.dosw.oficioya_users.entity.RoleEntity;
import edu.eci.dosw.oficioya_users.entity.UserEntity;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.mapper.UserEntityMapperImpl;
import edu.eci.dosw.oficioya_users.mapper.UserMapper;
import edu.eci.dosw.oficioya_users.repository.LocalityRepository;
import edu.eci.dosw.oficioya_users.repository.RoleRepository;
import edu.eci.dosw.oficioya_users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import edu.eci.dosw.oficioya_users.exception.ApiException;
import edu.eci.dosw.oficioya_users.exception.ServiceUnavailableException;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas de UserService con los repositorios y los clientes de Worker y Servicios simulados
 * (Mockito), los mappers son reales
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private WorkerClient workerClient;
    @Mock
    private RequestsClient requestsClient;
    @Mock
    private LocalityRepository localityRepository;

    private UserService userService;
    private final UUID adminId = UUID.randomUUID();

    /**
     * Método que se ejecuta antes de cada prueba y prepara el servicio con los mappers reales y las demás
     * dependencias simuladas.
     */
    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, new UserEntityMapperImpl(), new UserMapper(), workerClient, requestsClient, localityRepository);
    } // Cierre del método

    /**
     * Método que crea un rol.
     *
     * @param name El nombre del rol.
     * @return RoleEntity El rol.
     */
    private static RoleEntity role(String name) {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName(name);
        return role;
    } // Cierre del método

    /**
     * Método que crea un teléfono de petición.
     *
     * @param number El número.
     * @param principal Si es principal.
     * @return PhoneDTO El teléfono.
     */
    private static PhoneDTO phoneDTO(String number, boolean principal) {
        PhoneDTO dto = new PhoneDTO();
        dto.setNumber(number);
        dto.setPrincipal(principal);
        return dto;
    } // Cierre del método

    /**
     * Método que crea un teléfono guardado.
     *
     * @param number El número.
     * @param principal Si es principal.
     * @return PhoneEntity El teléfono.
     */
    private static PhoneEntity phone(String number, boolean principal) {
        PhoneEntity phone = new PhoneEntity();
        phone.setId(UUID.randomUUID());
        phone.setNumber(number);
        phone.setPrincipal(principal);
        return phone;
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
        user.setEmail("ana@mail.com");
        user.setAccountStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        user.setPhones(List.of(phone("3001234567", true)));
        Set<RoleEntity> set = new HashSet<>();
        for (String name : roles) {
            set.add(role(name));
        }
        user.setRoles(set);
        return user;
    } // Cierre del método

    /**
     * Método que crea un registro válido.
     *
     * @param roles Los roles pedidos.
     * @return UserRequestDTO El registro.
     */
    private static UserRequestDTO request(RoleName... roles) {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("  Ana Pérez ");
        request.setEmail("Ana@Mail.com");
        request.setPhones(new ArrayList<>(List.of(phoneDTO("3001234567", true))));
        request.setRoles(Set.of(roles));
        request.setBirthDate(LocalDate.of(1995, 5, 20));
        return request;
    } // Cierre del método

    /**
     * Método que simula que guardar una cuenta devuelve la misma cuenta.
     */
    private void saveReturnsArgument() {
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    } // Cierre del método

    /**
     * Método que verifica que una acción lance una ApiException con un código HTTP.
     *
     * @param action La acción que debe fallar.
     * @param status El código esperado.
     */
    private void assertStatus(Executable action, HttpStatus status) {
        ApiException exception = assertThrows(ApiException.class, action);
        assertEquals(status, exception.getStatus());
    } // Cierre del método

    /**
     * Prueba que el registro crea una cuenta activa y responde con los datos enmascarados.
     */
    @Test
    void registerCreatesActiveAccountWithMaskedData() {
        when(userRepository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(roleRepository.findByName("TRABAJADOR")).thenReturn(Optional.of(role("TRABAJADOR")));
        saveReturnsArgument();

        UserResponseDTO result = userService.register(request(RoleName.TRABAJADOR));

        assertThat(result.getName()).isEqualTo("Ana Pérez");
        assertThat(result.getEmail()).isEqualTo("a**a@mail.com");
        assertThat(result.getPhones().get(0).getNumber()).isEqualTo("******4567");
        assertThat(result.getRoles()).containsExactly("TRABAJADOR");
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    } // Cierre del método

    // RF-01 fecha de nacimiento (fase 1: opcional, si viene se valida)

    /**
     * Método que obtiene la fecha de hoy en Bogotá.
     *
     * @return LocalDate La fecha, para calcular edades en las pruebas.
     */
    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("America/Bogota"));
    } // Cierre del método

    /**
     * Método que crea un registro con una fecha de nacimiento dada.
     *
     * @param birthDate La fecha de nacimiento.
     * @return UserRequestDTO El registro.
     */
    private UserRequestDTO requestBornOn(LocalDate birthDate) {
        UserRequestDTO request = request(RoleName.CONTRATANTE);
        request.setBirthDate(birthDate);
        return request;
    } // Cierre del método

    /**
     * Prueba que se guarda la fecha de nacimiento de un mayor de edad y se muestra en el perfil propio.
     */
    @Test
    void registerSavesBirthDateOfAdultAndShowsItInOwnProfile() {
        when(roleRepository.findByName("CONTRATANTE")).thenReturn(Optional.of(role("CONTRATANTE")));
        saveReturnsArgument();
        LocalDate birthDate = today().minusYears(30);

        UserResponseDTO result = userService.register(requestBornOn(birthDate));

        assertThat(result.getBirthDate()).isEqualTo(birthDate);
    } // Cierre del método

    /**
     * Prueba que se acepta a quien cumple 18 años hoy.
     */
    @Test
    void registerAcceptsWhoTurnsEighteenToday() {
        when(roleRepository.findByName("CONTRATANTE")).thenReturn(Optional.of(role("CONTRATANTE")));
        saveReturnsArgument();

        UserResponseDTO result = userService.register(requestBornOn(today().minusYears(18)));

        assertThat(result.getBirthDate()).isEqualTo(today().minusYears(18));
    } // Cierre del método

    /**
     * Prueba que el registro exige la fecha de nacimiento.
     */
    @Test
    void registerRequiresBirthDate() {
        assertStatus(() -> userService.register(requestBornOn(null)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository);
    } // Cierre del método

    /**
     * Prueba que se rechazan menores de edad y fechas de hoy, futuras o anteriores a 1900.
     */
    @Test
    void registerRejectsMinorTodayFutureAndTooOldBirthDates() {
        Stream.of(today().minusYears(18).plusDays(1), today(), today().plusDays(1), LocalDate.of(1899, 12, 31))
                .forEach(date -> assertStatus(() -> userService.register(requestBornOn(date)), HttpStatus.BAD_REQUEST));
        verifyNoInteractions(userRepository);
    } // Cierre del método

    /**
     * Prueba que no se puede registrar un correo repetido (409).
     */
    @Test
    void registerRejectsDuplicatedEmail() {
        when(userRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertStatus(() -> userService.register(request(RoleName.TRABAJADOR)), HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que el rol ADMIN se rechaza antes de revisar el correo.
     */
    @Test
    void registerRejectsAdminRoleBeforeCheckingEmail() {
        assertStatus(() -> userService.register(request(RoleName.ADMIN)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository);
    } // Cierre del método

    /**
     * Prueba que el registro exige exactamente un teléfono principal.
     */
    @Test
    void registerRequiresExactlyOnePrincipalPhone() {
        UserRequestDTO request = request(RoleName.CONTRATANTE);
        request.setPhones(List.of(phoneDTO("3001234567", false)));

        assertStatus(() -> userService.register(request), HttpStatus.BAD_REQUEST);
    } // Cierre del método

    /**
     * Prueba que el registro rechaza números repetidos.
     */
    @Test
    void registerRejectsRepeatedPhones() {
        UserRequestDTO request = request(RoleName.CONTRATANTE);
        request.setPhones(List.of(phoneDTO("3001234567", true), phoneDTO("3001234567", false)));

        assertStatus(() -> userService.register(request), HttpStatus.BAD_REQUEST);
    } // Cierre del método

    /**
     * Prueba que el registro falla si el rol no está sembrado.
     */
    @Test
    void registerFailsWhenRoleIsNotSeeded() {
        when(roleRepository.findByName("CONTRATANTE")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> userService.register(request(RoleName.CONTRATANTE)));
    } // Cierre del método

    /**
     * Prueba que el perfil propio devuelve los datos enmascarados.
     */
    @Test
    void getOwnProfileReturnsMaskedData() {
        UserEntity user = user(AccountStatus.ACTIVE, "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(userService.getOwnProfile(user.getId()).getEmail()).isEqualTo("a**a@mail.com");
    } // Cierre del método

    /**
     * Prueba que el perfil propio falla si la cuenta no existe (404).
     */
    @Test
    void getOwnProfileFailsWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> userService.getOwnProfile(id), HttpStatus.NOT_FOUND);
    } // Cierre del método

    /**
     * Prueba que el perfil propio falla si la cuenta está inactiva (403).
     */
    @Test
    void getOwnProfileFailsWhenAccountIsInactive() {
        UserEntity user = user(AccountStatus.INACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.getOwnProfile(user.getId()), HttpStatus.FORBIDDEN);
    } // Cierre del método

    /**
     * Prueba que la edición cambia el nombre y los teléfonos.
     */
    @Test
    void updateOwnProfileChangesNameAndPhones() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();
        UserUpdateDTO request = new UserUpdateDTO();
        request.setName(" Ana María ");
        request.setPhones(List.of(phoneDTO("3109876543", true), phoneDTO("3201112233", false)));

        UserResponseDTO result = userService.updateOwnProfile(user.getId(), request);

        assertThat(result.getName()).isEqualTo("Ana María");
        assertThat(result.getPhones()).extracting(PhoneDTO::getNumber).containsExactly("******6543", "******2233");
    } // Cierre del método

    /**
     * Prueba que una edición sin campos deja los datos igual.
     */
    @Test
    void updateOwnProfileWithoutChangesKeepsData() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        assertThat(userService.updateOwnProfile(user.getId(), new UserUpdateDTO()).getName()).isEqualTo("Ana Pérez");
    } // Cierre del método

    // RF-14 localidad (trabajador y contratante, no admin)

    /**
     * Método que crea una localidad.
     *
     * @param id El id.
     * @param name El nombre.
     * @return LocalityEntity La localidad.
     */
    private static LocalityEntity locality(long id, String name) {
        LocalityEntity locality = new LocalityEntity();
        locality.setId(id);
        locality.setName(name);
        return locality;
    } // Cierre del método

    /**
     * Prueba que el registro guarda la localidad y la muestra.
     */
    @Test
    void registerSavesLocalityAndShowsIt() {
        when(roleRepository.findByName("CONTRATANTE")).thenReturn(Optional.of(role("CONTRATANTE")));
        when(localityRepository.findById(11L)).thenReturn(Optional.of(locality(11, "Suba")));
        saveReturnsArgument();
        UserRequestDTO request = request(RoleName.CONTRATANTE);
        request.setLocalityId(11L);

        UserResponseDTO result = userService.register(request);

        assertThat(result.getLocality().id()).isEqualTo(11L);
        assertThat(result.getLocality().name()).isEqualTo("Suba");
    } // Cierre del método

    /**
     * Prueba que el registro rechaza una localidad que no existe (400).
     */
    @Test
    void registerRejectsUnknownLocality() {
        when(localityRepository.findById(99L)).thenReturn(Optional.empty());
        UserRequestDTO request = request(RoleName.CONTRATANTE);
        request.setLocalityId(99L);

        assertStatus(() -> userService.register(request), HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que el registro guarda la localidad de un trabajador.
     */
    @Test
    void registerSavesLocalityForWorker() {
        when(roleRepository.findByName("TRABAJADOR")).thenReturn(Optional.of(role("TRABAJADOR")));
        when(localityRepository.findById(11L)).thenReturn(Optional.of(locality(11, "Suba")));
        saveReturnsArgument();
        UserRequestDTO request = request(RoleName.TRABAJADOR);
        request.setLocalityId(11L);

        assertThat(userService.register(request).getLocality().name()).isEqualTo("Suba");
    } // Cierre del método

    /**
     * Prueba que el trabajador puede cambiar su localidad.
     */
    @Test
    void updateOwnProfileChangesLocalityForWorker() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(localityRepository.findById(8L)).thenReturn(Optional.of(locality(8, "Kennedy")));
        saveReturnsArgument();
        UserUpdateDTO request = new UserUpdateDTO();
        request.setLocalityId(8L);

        assertThat(userService.updateOwnProfile(user.getId(), request).getLocality().name()).isEqualTo("Kennedy");
    } // Cierre del método

    /**
     * Prueba que el administrador no puede tener localidad (403).
     */
    @Test
    void updateOwnProfileRejectsLocalityForAdmin() {
        UserEntity admin = user(AccountStatus.ACTIVE, "ADMIN");
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        UserUpdateDTO request = new UserUpdateDTO();
        request.setLocalityId(8L);

        assertStatus(() -> userService.updateOwnProfile(admin.getId(), request), HttpStatus.FORBIDDEN);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que el trabajador puede editar su nombre sin enviar localidad.
     */
    @Test
    void workerCanStillEditNameWithoutLocality() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();
        UserUpdateDTO request = new UserUpdateDTO();
        request.setName("Ana María");

        assertThat(userService.updateOwnProfile(user.getId(), request).getName()).isEqualTo("Ana María");
    } // Cierre del método

    /**
     * Prueba que al quitar el rol CONTRATANTE se conserva la localidad.
     */
    @Test
    void removeContractorRoleKeepsLocality() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        user.setLocality(locality(11, "Suba"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        assertThat(userService.removeRole(adminId, user.getId(), RoleName.CONTRATANTE).getLocality().name()).isEqualTo("Suba");
    } // Cierre del método

    /**
     * Prueba que al quitar el rol TRABAJADOR se conserva la localidad.
     */
    @Test
    void removeWorkerRoleKeepsLocality() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        user.setLocality(locality(11, "Suba"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(workerClient.findWorkerIdByUser(user.getId())).thenReturn(Optional.empty());
        saveReturnsArgument();

        assertThat(userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR).getLocality().name()).isEqualTo("Suba");
    } // Cierre del método

    /**
     * Prueba que la edición cambia la localidad.
     */
    @Test
    void updateOwnProfileChangesLocality() {
        UserEntity user = user(AccountStatus.ACTIVE, "CONTRATANTE");
        user.setLocality(locality(11, "Suba"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(localityRepository.findById(8L)).thenReturn(Optional.of(locality(8, "Kennedy")));
        saveReturnsArgument();
        UserUpdateDTO request = new UserUpdateDTO();
        request.setLocalityId(8L);

        assertThat(userService.updateOwnProfile(user.getId(), request).getLocality().name()).isEqualTo("Kennedy");
    } // Cierre del método

    /**
     * Prueba que los teléfonos propios se devuelven completos.
     */
    @Test
    void getOwnPhonesReturnsFullNumbers() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<PhoneDTO> result = userService.getOwnPhones(user.getId());

        assertThat(result).extracting(PhoneDTO::getNumber).containsExactly("3001234567");
        assertThat(result.get(0).getId()).isNotNull();
    } // Cierre del método

    /**
     * Prueba que se agrega un teléfono adicional no principal.
     */
    @Test
    void addPhoneAddsAdditionalPhone() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        List<PhoneDTO> result = userService.addPhone(user.getId(), phoneDTO("3109876543", true));

        assertThat(result).extracting(PhoneDTO::isPrincipal).containsExactly(true, false);
        assertThat(result).extracting(PhoneDTO::getNumber).containsExactly("3001234567", "3109876543");
    } // Cierre del método

    /**
     * Prueba que los teléfonos guardados no salen repetidos en la respuesta.
     */
    @Test
    void savedPhonesAreNotRepeated() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        // Simula el merge de Hibernate devolviendo el teléfono nuevo dos veces
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            PhoneEntity added = saved.getPhones().get(1);
            added.setId(UUID.randomUUID());
            saved.getPhones().add(added);
            return saved;
        });

        List<PhoneDTO> result = userService.addPhone(user.getId(), phoneDTO("3109876543", false));

        assertThat(result).extracting(PhoneDTO::getNumber).containsExactly("3001234567", "3109876543");
    } // Cierre del método

    /**
     * Prueba que no se pueden tener más de 3 teléfonos.
     */
    @Test
    void addPhoneRejectsMoreThanThreePhones() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        user.setPhones(List.of(phone("3001234567", true), phone("3001111111", false), phone("3002222222", false)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.addPhone(user.getId(), phoneDTO("3109876543", false)), HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que no se puede agregar un número que ya está en la cuenta (409).
     */
    @Test
    void addPhoneRejectsRepeatedNumber() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.addPhone(user.getId(), phoneDTO("3001234567", false)), HttpStatus.CONFLICT);
    } // Cierre del método

    /**
     * Prueba que cambiar el número conserva la marca de principal.
     */
    @Test
    void updatePhoneChangesNumberAndKeepsPrincipal() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        UUID phoneId = user.getPhones().get(0).getId();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        List<PhoneDTO> result = userService.updatePhone(user.getId(), phoneId, phoneDTO("3109876543", false));

        assertThat(result.get(0).getNumber()).isEqualTo("3109876543");
        assertThat(result.get(0).isPrincipal()).isTrue();
    } // Cierre del método

    /**
     * Prueba que cambiar un teléfono que no existe falla (404).
     */
    @Test
    void updatePhoneFailsWhenPhoneDoesNotExist() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.updatePhone(user.getId(), UUID.randomUUID(), phoneDTO("3109876543", false)),
                HttpStatus.NOT_FOUND);
    } // Cierre del método

    /**
     * Prueba que se elimina un teléfono adicional.
     */
    @Test
    void removePhoneDeletesAdditionalPhone() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        user.setPhones(List.of(phone("3001234567", true), phone("3001111111", false)));
        UUID additionalId = user.getPhones().get(1).getId();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        List<PhoneDTO> result = userService.removePhone(user.getId(), additionalId);

        assertThat(result).extracting(PhoneDTO::getNumber).containsExactly("3001234567");
    } // Cierre del método

    /**
     * Prueba que no se puede eliminar el teléfono principal.
     */
    @Test
    void removePhoneRejectsPrincipal() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        UUID principalId = user.getPhones().get(0).getId();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.removePhone(user.getId(), principalId), HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que se agrega el rol que falta.
     */
    @Test
    void addRoleAddsMissingRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName("CONTRATANTE")).thenReturn(Optional.of(role("CONTRATANTE")));
        saveReturnsArgument();

        assertThat(userService.addRole(user.getId(), RoleName.CONTRATANTE).getRoles())
                .containsExactlyInAnyOrder("TRABAJADOR", "CONTRATANTE");
    } // Cierre del método

    /**
     * Prueba que no se puede agregar un rol que ya se tiene (409).
     */
    @Test
    void addRoleRejectsRoleAlreadyAssigned() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.addRole(user.getId(), RoleName.TRABAJADOR), HttpStatus.CONFLICT);
    } // Cierre del método

    /**
     * Prueba que no se puede agregar el rol ADMIN.
     */
    @Test
    void addRoleRejectsAdmin() {
        assertStatus(() -> userService.addRole(UUID.randomUUID(), RoleName.ADMIN), HttpStatus.BAD_REQUEST);
    } // Cierre del método

    /**
     * Prueba que el administrador no puede agregarse roles (403).
     */
    @Test
    void addRoleRejectsAdminAccount() {
        UserEntity admin = user(AccountStatus.ACTIVE, "ADMIN");
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertStatus(() -> userService.addRole(admin.getId(), RoleName.TRABAJADOR), HttpStatus.FORBIDDEN);
    } // Cierre del método

    /**
     * Prueba que el perfil público oculta los datos de contacto.
     */
    @Test
    void getPublicProfileHidesContactData() {
        UserEntity requester = user(AccountStatus.ACTIVE, "CONTRATANTE");
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        user.setBirthDate(LocalDate.of(1990, 1, 15));
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(userRepository.findByIdAndAccountStatus(user.getId(), AccountStatus.ACTIVE)).thenReturn(Optional.of(user));

        UserResponseDTO result = userService.getPublicProfile(requester.getId(), user.getId());

        assertThat(result.getName()).isEqualTo("Ana Pérez");
        assertThat(result.getEmail()).isNull();
        assertThat(result.getPhones()).isNull();
        assertThat(result.getBirthDate()).isNull();
    } // Cierre del método

    /**
     * Prueba que el perfil público falla si la cuenta no existe o está inactiva (404).
     */
    @Test
    void getPublicProfileFailsForMissingOrInactiveUser() {
        UserEntity requester = user(AccountStatus.ACTIVE, "CONTRATANTE");
        UUID id = UUID.randomUUID();
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(userRepository.findByIdAndAccountStatus(id, AccountStatus.ACTIVE)).thenReturn(Optional.empty());

        assertStatus(() -> userService.getPublicProfile(requester.getId(), id), HttpStatus.NOT_FOUND);
    } // Cierre del método

    /**
     * Prueba que una cuenta inactiva no puede consultar perfiles públicos (403).
     */
    @Test
    void getPublicProfileRejectsInactiveRequester() {
        UserEntity requester = user(AccountStatus.INACTIVE, "CONTRATANTE");
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));

        assertStatus(() -> userService.getPublicProfile(requester.getId(), UUID.randomUUID()), HttpStatus.FORBIDDEN);
    } // Cierre del método

    /**
     * Prueba que la consulta de varios usuarios devuelve los datos públicos de las cuentas activas.
     */
    @Test
    void findPublicByIdsReturnsPublicDataOfActiveUsers() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findByIdInAndAccountStatus(Set.of(user.getId()), AccountStatus.ACTIVE))
                .thenReturn(List.of(user));

        List<UserResponseDTO> result = userService.findPublicByIds(List.of(user.getId(), user.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(user.getId());
        assertThat(result.get(0).getName()).isEqualTo("Ana Pérez");
        assertThat(result.get(0).getRoles()).containsExactly("TRABAJADOR");
        assertThat(result.get(0).getEmail()).isNull();
        assertThat(result.get(0).getPhones()).isNull();
    } // Cierre del método

    /**
     * Prueba que se rechazan más de 50 ids.
     */
    @Test
    void findPublicByIdsRejectsMoreThanFiftyIds() {
        List<UUID> ids = Stream.generate(UUID::randomUUID).limit(51).toList();

        assertStatus(() -> userService.findPublicByIds(ids), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository);
    } // Cierre del método

    /**
     * Prueba que la vista del administrador tiene todos los datos.
     */
    @Test
    void adminSeesFullData() {
        UserEntity user = user(AccountStatus.INACTIVE, "TRABAJADOR");
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(userService.findAll().get(0).getEmail()).isEqualTo("ana@mail.com");
        assertThat(userService.findById(user.getId()).getPhones().get(0).getNumber()).isEqualTo("3001234567");
    } // Cierre del método

    /**
     * Prueba que la búsqueda por id falla si la cuenta no existe (404).
     */
    @Test
    void findByIdFailsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> userService.findById(id), HttpStatus.NOT_FOUND);
    } // Cierre del método

    /**
     * Prueba que desactivar y activar cambian el estado de la cuenta.
     */
    @Test
    void deactivateAndActivateChangeStatus() {
        UserEntity active = user(AccountStatus.ACTIVE, "TRABAJADOR");
        UserEntity inactive = user(AccountStatus.INACTIVE, "TRABAJADOR");
        inactive.setId(active.getId());
        when(userRepository.findById(active.getId())).thenReturn(Optional.of(active), Optional.of(inactive));
        saveReturnsArgument();

        assertThat(userService.changeStatus(adminId, active.getId(), AccountStatus.INACTIVE, "Mal uso")
                .getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(userService.changeStatus(adminId, active.getId(), AccountStatus.ACTIVE, "Resuelto")
                .getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    } // Cierre del método

    /**
     * Prueba que desactivar guarda la fecha de desactivación y activar la borra.
     */
    @Test
    void changeStatusSetsAndClearsDeactivationDate() {
        UserEntity active = user(AccountStatus.ACTIVE, "TRABAJADOR");
        UserEntity inactive = user(AccountStatus.INACTIVE, "TRABAJADOR");
        inactive.setId(active.getId());
        inactive.setDeactivatedAt(LocalDateTime.now().minusDays(3));
        when(userRepository.findById(active.getId())).thenReturn(Optional.of(active), Optional.of(inactive));
        saveReturnsArgument();
        LocalDateTime before = LocalDateTime.now();

        userService.changeStatus(adminId, active.getId(), AccountStatus.INACTIVE, "Mal uso");
        userService.changeStatus(adminId, active.getId(), AccountStatus.ACTIVE, "Resuelto");

        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getDeactivatedAt()).isAfterOrEqualTo(before);
        assertThat(saved.getAllValues().get(1).getDeactivatedAt()).isNull();
    } // Cierre del método

    /**
     * Prueba que las estadísticas cuentan por rol las registradas y desactivadas del periodo.
     */
    @Test
    void accountStatsCountsRegisteredUntilEndAndDeactivatedInPeriodByRole() {
        LocalDateTime start = LocalDate.of(2026, 11, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(2026, 12, 1).atStartOfDay();
        List<String> both = List.of("TRABAJADOR", "CONTRATANTE");
        when(userRepository.countRegistered(both, end)).thenReturn(1480L);
        when(userRepository.countRegistered(List.of("TRABAJADOR"), end)).thenReturn(620L);
        when(userRepository.countRegistered(List.of("CONTRATANTE"), end)).thenReturn(900L);
        when(userRepository.countDeactivated(both, start, end)).thenReturn(40L);
        when(userRepository.countDeactivated(List.of("TRABAJADOR"), start, end)).thenReturn(15L);
        when(userRepository.countDeactivated(List.of("CONTRATANTE"), start, end)).thenReturn(28L);

        AccountStatsDTO stats = userService.getAccountStats(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(stats.registered()).isEqualTo(new RoleCountDTO(1480, 620, 900));
        assertThat(stats.deactivated()).isEqualTo(new RoleCountDTO(40, 15, 28));
    } // Cierre del método

    /**
     * Prueba que un periodo de un solo día incluye el día completo.
     */
    @Test
    void accountStatsOfOneDayIncludesTheWholeDay() {
        LocalDate day = LocalDate.of(2026, 11, 16);

        userService.getAccountStats(day, day);

        verify(userRepository).countDeactivated(List.of("TRABAJADOR", "CONTRATANTE"),
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        verify(userRepository).countRegistered(List.of("TRABAJADOR", "CONTRATANTE"), day.plusDays(1).atStartOfDay());
    } // Cierre del método

    /**
     * Prueba que las estadísticas exigen ambas fechas y en orden.
     */
    @Test
    void accountStatsRequiresBothDatesInOrder() {
        LocalDate day = LocalDate.of(2026, 11, 1);

        assertStatus(() -> userService.getAccountStats(null, day), HttpStatus.BAD_REQUEST);
        assertStatus(() -> userService.getAccountStats(day, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> userService.getAccountStats(day.plusDays(1), day), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository);
    } // Cierre del método

    /**
     * Prueba que el administrador no puede desactivar su propia cuenta (403).
     */
    @Test
    void adminCannotDeactivateOwnAccount() {
        assertStatus(() -> userService.changeStatus(adminId, adminId, AccountStatus.INACTIVE, "Prueba"),
                HttpStatus.FORBIDDEN);
    } // Cierre del método

    /**
     * Prueba que activar la propia cuenta ya activa es un conflicto (409).
     */
    @Test
    void adminActivatingOwnActiveAccountIsConflict() {
        UserEntity admin = user(AccountStatus.ACTIVE, "ADMIN");
        admin.setId(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertStatus(() -> userService.changeStatus(adminId, adminId, AccountStatus.ACTIVE, "Prueba"),
                HttpStatus.CONFLICT);
    } // Cierre del método

    /**
     * Prueba que cambiar al mismo estado es un conflicto (409).
     */
    @Test
    void changeStatusToSameStatusIsConflict() {
        UserEntity user = user(AccountStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.changeStatus(adminId, user.getId(), AccountStatus.ACTIVE, "Prueba"),
                HttpStatus.CONFLICT);
    } // Cierre del método

    /**
     * Prueba que cambiar el estado de una cuenta que no existe falla (404).
     */
    @Test
    void changeStatusFailsWhenUserMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> userService.changeStatus(adminId, id, AccountStatus.INACTIVE, "Prueba"),
                HttpStatus.NOT_FOUND);
    } // Cierre del método

    /**
     * Prueba que la compensación elimina una cuenta existente.
     */
    @Test
    void deleteByIdRemovesExistingUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);
        userService.deleteById(id);
        verify(userRepository).deleteById(id);
    } // Cierre del método

    @Test
    void deleteByIdMissingUserReturns404() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);
        assertStatus(() -> userService.deleteById(id), HttpStatus.NOT_FOUND);
        verify(userRepository, never()).deleteById(any());
    }

    /**
     * Prueba que la búsqueda por correo ignora mayúsculas y espacios.
     */
    @Test
    void findByEmailReturnsFullAccountIgnoringCaseAndSpaces() {
        UserEntity user = user(AccountStatus.INACTIVE, "TRABAJADOR");
        when(userRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(user));

        UserResponseDTO found = userService.findByEmail("  Ana@Mail.com ");

        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(found.getEmail()).isEqualTo("ana@mail.com");
        assertThat(found.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(found.getRoles()).containsExactly("TRABAJADOR");
    } // Cierre del método

    /**
     * Prueba que la búsqueda por correo falla si no existe o está vacío.
     */
    @Test
    void findByEmailFailsWhenMissingOrBlank() {
        assertStatus(() -> userService.findByEmail("nadie@mail.com"), HttpStatus.NOT_FOUND);
        assertStatus(() -> userService.findByEmail("  "), HttpStatus.NOT_FOUND);
    } // Cierre del método
    // RF-02 y RF-13 con Worker (simulado)

    /**
     * Prueba que agregar TRABAJADOR reactiva el perfil existente en Worker.
     */
    @Test
    void addWorkerRoleReactivatesExistingWorkerProfile() {
        UserEntity user = user(AccountStatus.ACTIVE, "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(workerClient.findWorkerIdByUser(user.getId())).thenReturn(Optional.of("w-1"));
        when(roleRepository.findByName("TRABAJADOR")).thenReturn(Optional.of(role("TRABAJADOR")));
        saveReturnsArgument();

        assertThat(userService.addRole(user.getId(), RoleName.TRABAJADOR).getRoles())
                .containsExactlyInAnyOrder("CONTRATANTE", "TRABAJADOR");
        verify(workerClient).updateStatus("w-1", WorkerClient.ACTIVE);
    } // Cierre del método

    /**
     * Prueba que agregar TRABAJADOR sin perfil en Worker solo agrega el rol.
     */
    @Test
    void addWorkerRoleWithoutWorkerProfileOnlyAddsRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(workerClient.findWorkerIdByUser(user.getId())).thenReturn(Optional.empty());
        when(roleRepository.findByName("TRABAJADOR")).thenReturn(Optional.of(role("TRABAJADOR")));
        saveReturnsArgument();

        assertThat(userService.addRole(user.getId(), RoleName.TRABAJADOR).getRoles())
                .containsExactlyInAnyOrder("CONTRATANTE", "TRABAJADOR");
        verify(workerClient, never()).updateStatus(any(), any());
    } // Cierre del método

    /**
     * Prueba que quitar TRABAJADOR pausa el perfil en Worker.
     */
    @Test
    void removeWorkerRolePausesWorkerProfile() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(workerClient.findWorkerIdByUser(user.getId())).thenReturn(Optional.of("w-1"));
        saveReturnsArgument();

        assertThat(userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR).getRoles())
                .containsExactly("CONTRATANTE");
        verify(workerClient).updateStatus("w-1", WorkerClient.PAUSED);
    } // Cierre del método

    /**
     * Prueba que quitar TRABAJADOR sin perfil en Worker solo quita el rol.
     */
    @Test
    void removeWorkerRoleWithoutWorkerProfileOnlyRemovesRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(workerClient.findWorkerIdByUser(user.getId())).thenReturn(Optional.empty());
        saveReturnsArgument();

        assertThat(userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR).getRoles())
                .containsExactly("CONTRATANTE");
        verify(workerClient, never()).updateStatus(any(), any());
    } // Cierre del método

    /**
     * Prueba que quitar CONTRATANTE no llama a Worker.
     */
    @Test
    void removeContractorRoleDoesNotCallWorker() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        assertThat(userService.removeRole(adminId, user.getId(), RoleName.CONTRATANTE).getRoles())
                .containsExactly("TRABAJADOR");
        verifyNoInteractions(workerClient);
    } // Cierre del método

    /**
     * Prueba que no se puede quitar el único rol de la cuenta (409).
     */
    @Test
    void removeRoleRejectsLastRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR), HttpStatus.CONFLICT);
        verifyNoInteractions(workerClient, requestsClient);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    @Test
    void removeRoleReturns404WhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertStatus(() -> userService.removeRole(adminId, id, RoleName.TRABAJADOR), HttpStatus.NOT_FOUND);
        verifyNoInteractions(workerClient, requestsClient);
    }

    /**
     * Prueba que no se pueden quitar roles a una cuenta de administrador (403).
     */
    @Test
    void removeRoleRejectsAdminAccount() {
        UserEntity admin = user(AccountStatus.ACTIVE, "ADMIN");
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertStatus(() -> userService.removeRole(adminId, admin.getId(), RoleName.TRABAJADOR), HttpStatus.FORBIDDEN);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que no se puede quitar un rol que la cuenta no tiene (409).
     */
    @Test
    void removeRoleRejectsMissingRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertStatus(() -> userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR), HttpStatus.CONFLICT);
        verifyNoInteractions(workerClient, requestsClient);
    } // Cierre del método

    /**
     * Prueba que se puede quitar un rol a una cuenta inactiva y se devuelve la vista del administrador.
     */
    @Test
    void removeRoleAllowsInactiveAccountAndReturnsAdminView() {
        UserEntity user = user(AccountStatus.INACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        saveReturnsArgument();

        UserResponseDTO result = userService.removeRole(adminId, user.getId(), RoleName.CONTRATANTE);

        assertThat(result.getRoles()).containsExactly("TRABAJADOR");
        assertThat(result.getEmail()).isEqualTo("ana@mail.com");
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
    } // Cierre del método

    /**
     * Prueba que el mensaje de conflicto indica cuántas solicitudes activas tiene la cuenta en ese rol.
     */
    @Test
    void removeRoleConflictMessageTalksAboutTheAccount() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(requestsClient.countActive(user.getId(), RoleName.TRABAJADOR)).thenReturn(2L);

        ApiException exception = assertThrows(ApiException.class,
                () -> userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR));
        assertThat(exception.getMessage()).isEqualTo("La cuenta tiene 2 solicitudes activas como TRABAJADOR; "
                + "deben finalizarse o cancelarse antes de quitar el rol");
    } // Cierre del método

    /**
     * Prueba que no se puede quitar el rol ADMIN (400).
     */
    @Test
    void removeRoleRejectsAdminRole() {
        assertStatus(() -> userService.removeRole(adminId, UUID.randomUUID(), RoleName.ADMIN), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(userRepository, workerClient, requestsClient);
    } // Cierre del método

    /**
     * Prueba que si Worker no responde se lanza 503 y el rol se conserva.
     */
    @Test
    void removeWorkerRoleFailsAndKeepsRoleWhenWorkerIsDown() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(workerClient.findWorkerIdByUser(user.getId())).thenThrow(new ServiceUnavailableException("caído"));

        assertStatus(() -> userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR), HttpStatus.SERVICE_UNAVAILABLE);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que no se puede quitar un rol con solicitudes activas en ese rol (409).
     */
    @Test
    void removeRoleRejectsWhenUserHasActiveRequestsInThatRole() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(requestsClient.countActive(user.getId(), RoleName.TRABAJADOR)).thenReturn(2L);

        assertStatus(() -> userService.removeRole(adminId, user.getId(), RoleName.TRABAJADOR), HttpStatus.CONFLICT);
        verifyNoInteractions(workerClient);
        verify(userRepository, never()).save(any());
    } // Cierre del método

    /**
     * Prueba que si Servicios no responde se lanza 503 y el rol se conserva.
     */
    @Test
    void removeRoleFailsAndKeepsRoleWhenRequestsServiceIsDown() {
        UserEntity user = user(AccountStatus.ACTIVE, "TRABAJADOR", "CONTRATANTE");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(requestsClient.countActive(user.getId(), RoleName.CONTRATANTE))
                .thenThrow(new ServiceUnavailableException("caído"));

        assertStatus(() -> userService.removeRole(adminId, user.getId(), RoleName.CONTRATANTE), HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(workerClient);
        verify(userRepository, never()).save(any());
    } // Cierre del método
} // Cierre de la clase