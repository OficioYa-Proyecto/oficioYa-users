package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.client.WorkerClient;
import edu.eci.dosw.oficioya_users.client.RequestsClient;
import edu.eci.dosw.oficioya_users.dto.AccountStatsDTO;
import edu.eci.dosw.oficioya_users.dto.PhoneDTO;
import edu.eci.dosw.oficioya_users.dto.RoleCountDTO;
import edu.eci.dosw.oficioya_users.dto.UserRequestDTO;
import edu.eci.dosw.oficioya_users.dto.UserResponseDTO;
import edu.eci.dosw.oficioya_users.dto.UserUpdateDTO;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import edu.eci.dosw.oficioya_users.enums.RoleName;
import edu.eci.dosw.oficioya_users.exception.BadRequestException;
import edu.eci.dosw.oficioya_users.exception.ConflictException;
import edu.eci.dosw.oficioya_users.exception.ForbiddenOperationException;
import edu.eci.dosw.oficioya_users.exception.ResourceNotFoundException;
import edu.eci.dosw.oficioya_users.mapper.UserEntityMapper;
import edu.eci.dosw.oficioya_users.mapper.UserMapper;
import edu.eci.dosw.oficioya_users.model.LocalityModel;
import edu.eci.dosw.oficioya_users.model.PhoneModel;
import edu.eci.dosw.oficioya_users.model.RoleModel;
import edu.eci.dosw.oficioya_users.model.UserModel;
import edu.eci.dosw.oficioya_users.repository.LocalityRepository;
import edu.eci.dosw.oficioya_users.repository.RoleRepository;
import edu.eci.dosw.oficioya_users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final int MAX_PHONES = 3;
    private static final int MAX_IDS = 50;
    private static final int ADULT_AGE = 18;
    private static final LocalDate MIN_BIRTH_DATE = LocalDate.of(1900, 1, 1);
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserEntityMapper userEntityMapper;
    private final UserMapper userMapper;
    private final WorkerClient workerClient;
    private final RequestsClient requestsClient;
    private final LocalityRepository localityRepository;

    /** RF-01 */
    public UserResponseDTO register(UserRequestDTO request) {
        request.getRoles().forEach(this::checkNotAdmin);
        validatePhones(request.getPhones());
        validateBirthDate(request.getBirthDate());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe una cuenta con ese correo");
        }

        UserModel user = new UserModel();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setBirthDate(request.getBirthDate());
        // RF-14. Solo el contratante tiene localidad; si no lo es, no se guarda
        if (request.getLocalityId() != null && request.getRoles().contains(RoleName.CONTRATANTE)) {
            user.setLocality(findLocality(request.getLocalityId()));
        }
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPhones(userMapper.toPhones(request.getPhones()));
        user.setRoles(request.getRoles().stream().map(this::findRole).collect(Collectors.toSet()));

        UserModel saved = save(user);
        log.info("Cuenta registrada {} con roles {}", saved.getId(), request.getRoles());
        return userMapper.toOwn(saved);
    }

    /** RF-02. Al agregar TRABAJADOR reactiva su perfil en Worker si ya existía */
    public UserResponseDTO addRole(UUID userId, RoleName roleName) {
        checkNotAdmin(roleName);
        UserModel user = getActiveUser(userId);

        if (hasRole(user, RoleName.ADMIN)) {
            throw new ForbiddenOperationException("El administrador no puede agregarse roles");
        }

        if (hasRole(user, roleName)) {
            throw new ConflictException("La cuenta ya tiene el rol " + roleName);
        }
        if (roleName == RoleName.TRABAJADOR) {
            changeWorkerStatus(userId, WorkerClient.ACTIVE);
        }
        user.getRoles().add(findRole(roleName));
        log.info("Rol {} agregado a la cuenta {}", roleName, userId);
        return userMapper.toOwn(save(user));
    }

    /**
     * RF-13. Solo el administrador quita TRABAJADOR o CONTRATANTE (al aprobar una petición o directamente,
     * vía RoleRemovalService); la cuenta conserva al menos un rol. Se permite aunque la cuenta esté inactiva.
     * Riesgo conocido: si Worker pausa el perfil y luego falla el guardado, el perfil queda pausado con el rol
     */
    public UserResponseDTO removeRole(UUID adminId, UUID userId, RoleName roleName) {
        if (roleName == RoleName.ADMIN) {
            throw new BadRequestException("El rol ADMIN no se puede quitar");
        }
        UserModel user = findUser(userId);

        if (hasRole(user, RoleName.ADMIN)) {
            throw new ForbiddenOperationException("No se pueden quitar roles a una cuenta de administrador");
        }
        if (!hasRole(user, roleName)) {
            throw new ConflictException("La cuenta no tiene el rol " + roleName);
        }
        if (user.getRoles().size() == 1) {
            throw new ConflictException("La cuenta debe conservar al menos un rol");
        }
        long activeRequests = requestsClient.countActive(userId, roleName);
        if (activeRequests > 0) {
            throw new ConflictException("La cuenta tiene " + activeRequests + " solicitudes activas como " + roleName
                    + "; deben finalizarse o cancelarse antes de quitar el rol");
        }
        if (roleName == RoleName.TRABAJADOR) {
            changeWorkerStatus(userId, WorkerClient.PAUSED);
        }
        user.getRoles().removeIf(role -> role.getName().equals(roleName.name()));
        // RF-14. La localidad es solo del contratante: al quitarle ese rol queda vacía
        if (roleName == RoleName.CONTRATANTE) {
            user.setLocality(null);
        }
        log.info("Admin {} quitó el rol {} de la cuenta {}", adminId, roleName, userId);
        return userMapper.toAdmin(save(user));
    }

    /** RF-03 */
    @Transactional(readOnly = true)
    public UserResponseDTO getOwnProfile(UUID userId) {
        return userMapper.toOwn(getActiveUser(userId));
    }

    /** RF-04 */
    public UserResponseDTO updateOwnProfile(UUID userId, UserUpdateDTO request) {
        UserModel user = getActiveUser(userId);
        if (request.getName() != null) {
            user.setName(request.getName().trim());
        }
        if (request.getPhones() != null) {
            validatePhones(request.getPhones());
            user.setPhones(userMapper.toPhones(request.getPhones()));
        }
        if (request.getLocalityId() != null) {
            // RF-14. Solo el contratante puede cambiar su localidad (ni el trabajador ni el admin)
            if (!hasRole(user, RoleName.CONTRATANTE)) {
                throw new ForbiddenOperationException("Solo el contratante puede cambiar su localidad");
            }
            user.setLocality(findLocality(request.getLocalityId()));
        }
        log.info("Perfil actualizado {}", userId);
        return userMapper.toOwn(save(user));
    }

    /** RF-04: teléfonos completos del dueño, para editarlos */
    @Transactional(readOnly = true)
    public List<PhoneDTO> getOwnPhones(UUID userId) {
        return userMapper.toPhoneDTOs(getActiveUser(userId).getPhones());
    }

    /** RF-04: agrega un teléfono adicional */
    public List<PhoneDTO> addPhone(UUID userId, PhoneDTO request) {
        UserModel user = getActiveUser(userId);
        if (user.getPhones().size() >= MAX_PHONES) {
            throw new BadRequestException("Se admiten máximo " + MAX_PHONES + " teléfonos");
        }
        checkNumberAvailable(user, request.getNumber(), null);

        PhoneModel phone = new PhoneModel();
        phone.setNumber(request.getNumber());
        phone.setPrincipal(false);
        List<PhoneModel> phones = new ArrayList<>(user.getPhones());
        phones.add(phone);
        user.setPhones(phones);
        log.info("Teléfono adicional agregado a la cuenta {}", userId);
        return userMapper.toPhoneDTOs(save(user).getPhones());
    }

    /** RF-04: cambia el número de un teléfono (principal o adicional) */
    public List<PhoneDTO> updatePhone(UUID userId, UUID phoneId, PhoneDTO request) {
        UserModel user = getActiveUser(userId);
        PhoneModel phone = findPhone(user, phoneId);
        checkNumberAvailable(user, request.getNumber(), phone);
        phone.setNumber(request.getNumber());
        log.info("Teléfono {} actualizado en la cuenta {}", phoneId, userId);
        return userMapper.toPhoneDTOs(save(user).getPhones());
    }

    /** RF-04: elimina un teléfono adicional */
    public List<PhoneDTO> removePhone(UUID userId, UUID phoneId) {
        UserModel user = getActiveUser(userId);
        PhoneModel phone = findPhone(user, phoneId);
        if (phone.isPrincipal()) {
            throw new BadRequestException("No se puede eliminar el teléfono principal");
        }
        List<PhoneModel> phones = new ArrayList<>(user.getPhones());
        phones.remove(phone);
        user.setPhones(phones);
        log.info("Teléfono {} eliminado de la cuenta {}", phoneId, userId);
        return userMapper.toPhoneDTOs(save(user).getPhones());
    }

    /** RF-05 */
    @Transactional(readOnly = true)
    public UserResponseDTO getPublicProfile(UUID requesterId, UUID id) {
        getActiveUser(requesterId);
        return userRepository.findByIdAndAccountStatus(id, AccountStatus.ACTIVE)
                .map(userEntityMapper::toModel)
                .map(userMapper::toPublic)
                .orElseThrow(() -> notFound(id));
    }

    /** RF-10 */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findPublicByIds(List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids);
        if (unique.size() > MAX_IDS) {
            throw new BadRequestException("Se pueden consultar máximo " + MAX_IDS + " usuarios por petición");
        }
        return userRepository.findByIdInAndAccountStatus(unique, AccountStatus.ACTIVE).stream()
                .map(userEntityMapper::toModel)
                .map(userMapper::toPublic)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(userEntityMapper::toModel)
                .map(userMapper::toAdmin)
                .toList();
    }

    /** RF-11 */
    @Transactional(readOnly = true)
    public UserResponseDTO findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(userEntityMapper::toModel)
                .map(userMapper::toAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    /** RF-08 */
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        return userMapper.toAdmin(findUser(id));
    }

    /** RF-06, RF-07 */
    public UserResponseDTO changeStatus(UUID adminId, UUID userId, AccountStatus status, String reason) {
        if (adminId.equals(userId) && status == AccountStatus.INACTIVE) {
            throw new ForbiddenOperationException("Un administrador no puede desactivar su propia cuenta");
        }
        UserModel user = findUser(userId);
        if (user.getAccountStatus() == status) {
            throw new ConflictException("La cuenta ya está en estado " + status);
        }
        user.setAccountStatus(status);
        user.setDeactivatedAt(status == AccountStatus.INACTIVE ? LocalDateTime.now() : null);
        log.info("Admin {} cambió la cuenta {} a {}. Motivo: {}", adminId, userId, status, reason);
        return userMapper.toAdmin(save(user));
    }

    /**
     * RF-12. Para Reporting: cuentas registradas (activas o no) y cuentas desactivadas entre.
     * Los administradores no cuentan porque no tienen rol de cuenta
     */
    public AccountStatsDTO getAccountStats(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Las fechas from y to son obligatorias (formato AAAA-MM-DD)");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("La fecha from no puede ser posterior a to");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        AccountStatsDTO stats = new AccountStatsDTO(
                countByRole(roles -> userRepository.countRegistered(roles, end)),
                countByRole(roles -> userRepository.countDeactivated(roles, start, end)));
        log.info("Estadísticas de cuentas del {} al {}: {} registradas, {} desactivadas",
                from, to, stats.registered().total(), stats.deactivated().total());
        return stats;
    }

    public void deleteById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw notFound(id);
        }
        userRepository.deleteById(id);
        log.info("Perfil {} eliminado por compensación del registro", id);
    }

    // Persistencia

    private UserModel findUser(UUID id) {
        return userRepository.findById(id)
                .map(userEntityMapper::toModel)
                .orElseThrow(() -> notFound(id));
    }

    private UserModel save(UserModel user) {
        UserModel saved = userEntityMapper.toModel(userRepository.save(userEntityMapper.toEntity(user)));
        // Hibernate 6.5 (HHH-5855) puede repetir en memoria un teléfono nuevo al hacer merge; en BD queda una vez
        Set<UUID> ids = new HashSet<>();
        saved.setPhones(saved.getPhones().stream()
                .filter(phone -> phone.getId() == null || ids.add(phone.getId()))
                .toList());
        return saved;
    }

    private static RoleCountDTO countByRole(ToLongFunction<Collection<String>> counter) {
        String worker = RoleName.TRABAJADOR.name();
        String contractor = RoleName.CONTRATANTE.name();
        return new RoleCountDTO(counter.applyAsLong(List.of(worker, contractor)),
                counter.applyAsLong(List.of(worker)), counter.applyAsLong(List.of(contractor)));
    }

    private RoleModel findRole(RoleName roleName) {
        return roleRepository.findByName(roleName.name())
                .map(userEntityMapper::toModel)
                .orElseThrow(() -> new IllegalStateException("Rol no configurado: " + roleName));
    }

    /** RF-14 */
    private LocalityModel findLocality(Long localityId) {
        return localityRepository.findById(localityId)
                .map(userEntityMapper::toModel)
                .orElseThrow(() -> new BadRequestException("La localidad no existe: " + localityId));
    }

    // Reglas de Negocio

    private UserModel getActiveUser(UUID id) {
        UserModel user = findUser(id);
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenOperationException("La cuenta está inactiva");
        }
        return user;
    }

    private boolean hasRole(UserModel user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName.name()));
    }

    /** Integración con Worker: si el usuario no tiene perfil de trabajador (404) no se hace nada */
    private void changeWorkerStatus(UUID userId, String status) {
        workerClient.findWorkerIdByUser(userId)
                .ifPresent(workerId -> workerClient.updateStatus(workerId, status));
    }

    private void checkNotAdmin(RoleName roleName) {
        if (roleName == RoleName.ADMIN) {
            throw new BadRequestException("El rol ADMIN no se puede asignar desde la cuenta");
        }
    }

    /** RF-01. 18 años cumplidos a la fecha de hoy en Bogotá */
    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new BadRequestException("La fecha de nacimiento es obligatoria");
        }
        LocalDate today = LocalDate.now(BOGOTA);
        if (birthDate.isBefore(MIN_BIRTH_DATE) || !birthDate.isBefore(today)) {
            throw new BadRequestException("La fecha de nacimiento no es válida");
        }
        if (Period.between(birthDate, today).getYears() < ADULT_AGE) {
            throw new BadRequestException("Debes ser mayor de edad para registrarte");
        }
    }

    private void validatePhones(List<PhoneDTO> phones) {
        if (phones.stream().filter(PhoneDTO::isPrincipal).count() != 1) {
            throw new BadRequestException("Debe haber exactamente un teléfono principal");
        }
        if (phones.stream().map(PhoneDTO::getNumber).distinct().count() != phones.size()) {
            throw new BadRequestException("Hay números de teléfono repetidos");
        }
    }

    private PhoneModel findPhone(UserModel user, UUID phoneId) {
        return user.getPhones().stream()
                .filter(phone -> phoneId.equals(phone.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Teléfono no encontrado: " + phoneId));
    }

    private void checkNumberAvailable(UserModel user, String number, PhoneModel excluded) {
        if (user.getPhones().stream().anyMatch(phone -> phone != excluded && phone.getNumber().equals(number))) {
            throw new ConflictException("Ese número ya está registrado en la cuenta");
        }
    }

    private ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException("Usuario no encontrado: " + id);
    }
}