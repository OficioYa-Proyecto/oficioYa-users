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

/**
 * Clase que guarda los datos con los repositorios, convierte con mappers y se comunica
 * con los microservicios de Worker y Servicios. Además de contener las reglas de negocio
 * para las cuentas de un usuario
 * Es usado por UserController y RoleRemovalService.
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

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

    /**
     * Método que registra una nueva cuenta y activa (RF-01). Valida
     * que no se pida rol ADMIN, un solo teléfono principal sin números repetidos,
     * el usuario sea mayor de edad y que el correo no exista.
     *
     * @param request Datos de registro (nombre, correo, etc)
     * @return UserResponseDTO La cuenta creada con los datos
     * para que Orquestador siga con el registro por medio del microservicio de Identity
     */
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
        if (request.getLocalityId() != null) {
            user.setLocality(findLocality(request.getLocalityId()));
        }
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPhones(userMapper.toPhones(request.getPhones()));
        user.setRoles(request.getRoles().stream().map(this::findRole).collect(Collectors.toSet()));

        UserModel saved = save(user);
        log.info("Cuenta registrada {} con roles {}", saved.getId(), request.getRoles());
        return userMapper.toOwn(saved);
    } // Cierre del método

    /**
     * Método que agrega a la cuenta el rol faltante (RF-02). Si el rol es
     * TRABAJADOR y el usuario ya tenía perfil en el microservicio de Worker, lo reactiva.
     *
     * @param userId El id del usuario autenticado (sub del token)
     * @param roleName El rol a agregar (CONTRATANTE o TRABAJADOR)
     * @return UserResponseDTO El perfil propio con el nuevo rol
     */
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
    } // Cierre del método

    /**
     * Método que quita un rol a una cuenta (solo usable por el admin), al aprobar una solicitud (RF-13).
     * La cuenta debe conservar al menos un rol y no tener solicitudes activas en dicho rol, si es TRABAJADOR
     * el perfil se pausa en el microservicio de Worker.
     *
     * @param adminId El id del administrador que quita el rol
     * @param userId El id de la cuenta a la que se le quita el rol
     * @param roleName El rol que se quita (CONTRATANTE o TRABAJADOR)
     * @return UserResponseDTO La cuenta completa sin el rol
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
        log.info("Admin {} quitó el rol {} de la cuenta {}", adminId, roleName, userId);
        return userMapper.toAdmin(save(user));
    } // Cierre del método

    /**
     * Método que obtiene el perfil propio del usuario autenticado (RF-03)
     *
     * @param userId El id del usuario autenticado (sub del token)
     * @return UserResponseDTO El perfil con correo y teléfono ocultos para mostrarlos
     * en la pantalla del perfil
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getOwnProfile(UUID userId) {
        return userMapper.toOwn(getActiveUser(userId));
    } // Cierre del método

    /**
     * Método que edita el perfil propio (RF-04). Solo cambia lo que se envía:
     * nombre, teléfonos y localidad (RF-14) [Administrador no tiene localidad]
     *
     * @param userId El id del usuario autenticado (sub del token)
     * @param request Los campos a modificar
     * @return UserResponseDTO El perfil actualizado
     */
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
            if (hasRole(user, RoleName.ADMIN)) {
                throw new ForbiddenOperationException("El administrador no tiene localidad");
            }
            user.setLocality(findLocality(request.getLocalityId()));
        }
        log.info("Perfil actualizado {}", userId);
        return userMapper.toOwn(save(user));
    } // Cierre del método

    /**
     * Método que obtiene los teléfonos propios sin ocultar (RF-04)
     *
     * @param userId El id del usuario autenticado (sub del token)
     * @return List<PhoneDTO> Los teléfonos completos con su id para que el
     * dueño pueda cambiarlos o eliminarlos
     */
    @Transactional(readOnly = true)
    public List<PhoneDTO> getOwnPhones(UUID userId) {
        return userMapper.toPhoneDTOs(getActiveUser(userId).getPhones());
    } // Cierre del método

    /**
     * Método que agrega un teléfono adicional hasta un máximo de 3 teléfonos y
     * sin números repetidos (RF-04)
     *
     * @param userId El id del usuario autenticado (sub del token)
     * @param request El teléfono nuevo (solo se usa el número)
     * @return List<PhoneDTO> Los teléfonos actualizados
     */
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
    } // Cierre del método

    /**
     * Método que cambia el número de un teléfono propio, principal o adicional (RF-04)
     *
     * @param userId El id del usuario autenticado (sub del token)
     * @param phoneId El id del teléfono que se va a cambiar
     * @param request El teléfono con el número nuevo
     * @return List<PhoneDTO> Los teléfonos actualizados
     */
    public List<PhoneDTO> updatePhone(UUID userId, UUID phoneId, PhoneDTO request) {
        UserModel user = getActiveUser(userId);
        PhoneModel phone = findPhone(user, phoneId);
        checkNumberAvailable(user, request.getNumber(), phone);
        phone.setNumber(request.getNumber());
        log.info("Teléfono {} actualizado en la cuenta {}", phoneId, userId);
        return userMapper.toPhoneDTOs(save(user).getPhones());
    } // Cierre del método

    /**
     * Método que elimina un teléfono adicional. El principal no se puede eliminar (RF-04)
     *
     * @param userId El id del usuario autenticado (sub del token)
     * @param phoneId El id del teléfono que se va a eliminar
     * @return List<PhoneDTO> Los teléfonos que quedan en la lista
     */
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
    } // Cierre del método

    /**
     * Método que obtiene el perfil público de otra cuenta activa, quien consulta
     * también debe tener la cuenta activa (RF-05)
     *
     * @param requesterId El id del usuario autenticado que consulta
     * @param id El id de la cuenta a ver
     * @return UserResponseDTO El perfil sin correo ni teléfonos, para que
     * otros usuarios lo vean sin exponer datos de contacto
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getPublicProfile(UUID requesterId, UUID id) {
        getActiveUser(requesterId);
        return userRepository.findByIdAndAccountStatus(id, AccountStatus.ACTIVE)
                .map(userEntityMapper::toModel)
                .map(userMapper::toPublic)
                .orElseThrow(() -> notFound(id));
    } // Cierre del método

    /**
     * Método que obtiene los datos públicos de varias cuentas activas a la vez,
     * las cuentas inactivas o inexistentes no se devuelven (RF-10)
     *
     * @param ids Los ids de las cuentas (máximo 50, repetidos se ignoran)
     * @return List<UserResponseDTO> Los perfiles públicos encontrados
     * [para que los microservicios de Workers y Services muestren nombres sin
     * hacer una solicitud por usuarios]
     */
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
    } // Cierre del método

    /**
     * Método que obtiene las cuentas con sus datos completos
     *
     * @return List<UserResponseDTO> Todas las cuentas activas o inactivas
     * [para listado del admin]
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(userEntityMapper::toModel)
                .map(userMapper::toAdmin)
                .toList();
    } // Cierre del método

    /**
     * Método que busca una cuenta por correo, sin importar mayúsculas,
     * espacios ni estado (RF-11)
     *
     * @param email El correo de la cuenta
     * @return UserResponseDTO La cuenta completa con su estado
     * [para que el microservicio de Identity sepa si puede iniciar sesión]
     */
    @Transactional(readOnly = true)
    public UserResponseDTO findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(userEntityMapper::toModel)
                .map(userMapper::toAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    } // Cierre del método

    /**
     * Método que busca una cuenta por id sin importar su estado (RF-08, RF-11)
     *
     * @param id El id de la cuenta
     * @return UserResponseDTO La cuenta completa
     * [para detalles del admin y consulta interna del microservicio de Identity]
     */
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        return userMapper.toAdmin(findUser(id));
    } // Cierre del método

    /**
     * Método que activa o desactiva una cuenta, un admin no puede desactivar su propia
     * cuenta (RF-06, RF-07)
     * [guarda fecha de desactivación para estadísticas del microservicio de Reporting]
     *
     * @param adminId El id del administrador que hace el cambio
     * @param userId El id de la cuenta que se cambia
     * @param status El nuevo estado (ACTIVE o INACTIVE)
     * @param reason El motivo del cambio (queda en el log)
     * @return UserResponseDTO La cuenta con su nuevo estado, para actualizar
     * el detalle del admin
     */
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
    } // Cierre del método

    /**
     * Método que calcula las estadísticas de cuentas para el microservicio de Reporting (RF-12)
     * cuentas registradas hasta (activas o no) y cuentas desactivadas entre y hasta,
     * en total y por rol [los admin se excluyen porque no tienen rol de cuenta]
     *
     * @param from La fecha de inicio del periodo
     * @param to La fecha de fin del periodo
     * @return AccountStatsDTO Los conteos de registradas y desactivadas
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
    } // Cierre del método

    /**
     * Método que elimina físicamente un perfil.
     * El Orquestador lo llama si el microservicio de Identity no pudo guardar
     * credenciales, para no dejar perfiles sin contraseña
     *
     * @param id El id del perfil a eliminar
     */
    public void deleteById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw notFound(id);
        }
        userRepository.deleteById(id);
        log.info("Perfil {} eliminado por compensación del registro", id);
    } // Cierre del método

    // Persistencia

    /**
     * Método que busca una cuenta por id, sin importar su estado
     *
     * @param id El id de la cuenta
     * @return UserModel La cuenta como modelo de dominio, para aplicar reglas de negocio
     */
    private UserModel findUser(UUID id) {
        return userRepository.findById(id)
                .map(userEntityMapper::toModel)
                .orElseThrow(() -> notFound(id));
    } // Cierre del método

    /**
     * Método que guarda una cuenta convirtiendo el modelo a entidad y de vuelta.
     * [Quita en memoria los teléfonos repetidos que Hibernate 6.5 (HHH-5855)
     * puede generar al hacer merge; en la base de datos quedan una sola vez.]
     *
     * @param user La cuenta a guardar
     * @return UserModel La cuenta con sus ids y fechas para devolverla al cliente
     */
    private UserModel save(UserModel user) {
        UserModel saved = userEntityMapper.toModel(userRepository.save(userEntityMapper.toEntity(user)));
        Set<UUID> ids = new HashSet<>();
        saved.setPhones(saved.getPhones().stream()
                .filter(phone -> phone.getId() == null || ids.add(phone.getId()))
                .toList());
        return saved;
    } // Cierre del método

    /**
     * Método que cuenta los perfiles en total, solo trabajadores y contratantes.
     * Un perfil con ambos roles cuenta en los dos por lo que la suma puede ser menor
     *
     * @param counter La consulta de conteo que recibe la lista de roles
     * @return RoleCountDTO Los tres conteos para armar estadísticas
     */
    private static RoleCountDTO countByRole(ToLongFunction<Collection<String>> counter) {
        String worker = RoleName.TRABAJADOR.name();
        String contractor = RoleName.CONTRATANTE.name();
        return new RoleCountDTO(counter.applyAsLong(List.of(worker, contractor)),
                counter.applyAsLong(List.of(worker)), counter.applyAsLong(List.of(contractor)));
    } // Cierre del método

    /**
     * Método que busca un rol en la base de datos
     *
     * @param roleName El nombre del rol
     * @return RoleModel El rol para asignar a la cuenta
     */
    private RoleModel findRole(RoleName roleName) {
        return roleRepository.findByName(roleName.name())
                .map(userEntityMapper::toModel)
                .orElseThrow(() -> new IllegalStateException("Rol no configurado: " + roleName));
    } // Cierre del método

    /**
     * Método que busca una localidad del catálogo (RF-14)
     *
     * @param localityId EL id de la localidad
     * @return LocalityModel La localidad para asignar a la cuenta
     */
    private LocalityModel findLocality(Long localityId) {
        return localityRepository.findById(localityId)
                .map(userEntityMapper::toModel)
                .orElseThrow(() -> new BadRequestException("La localidad no existe: " + localityId));
    } // Cierre del método

    // Reglas de Negocio

    /**
     * Método que busca una cuenta y verifica que esté activa
     *
     * @param id El id de la cuenta
     * @return UserModel La cuenta activa para que solo dichas cuentas puedan
     * usar su perfil
     */
    private UserModel getActiveUser(UUID id) {
        UserModel user = findUser(id);
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenOperationException("La cuenta está inactiva");
        }
        return user;
    } // Cierre del método

    /**
     * Método que verifica si una cuenta tiene un rol
     *
     * @param user La cuenta a revisar
     * @param roleName El rol que se busca
     * @return boolean Verifica si la cuenta tiene algún rol
     */
    private boolean hasRole(UserModel user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName.name()));
    } // Cierre del método

    /**
     * Método que cambia el estado del perfil de trabajador en el microservicio de Workers.
     * Si el usuario no tiene perfil de trabajador manda un error HTTP 404 y no se hace nada
     *
     * @param userId El id de la cuenta
     * @param status El nuevo estado del perfil (ACTIVE o PAUSED)
     */
    private void changeWorkerStatus(UUID userId, String status) {
        workerClient.findWorkerIdByUser(userId)
                .ifPresent(workerId -> workerClient.updateStatus(workerId, status));
    } // Cierre del método

    /**
     * Método que impide asignar el rol ADMIN desde la cuenta
     *
     * @param roleName El rol que se quiere asignar
     */
    private void checkNotAdmin(RoleName roleName) {
        if (roleName == RoleName.ADMIN) {
            throw new BadRequestException("El rol ADMIN no se puede asignar desde la cuenta");
        }
    } // Cierre del método

    /**
     * Método que valida la fecha de nacimiento (RF-01)
     *
     * @param birthDate La fecha de nacimiento
     */
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
    } // Cierre del método

    /**
     * Método que valida que haya exctamente un teléfono principal y que no haya
     * números repetidos
     *
     * @param phones Los teléfonos a validar
     */
    private void validatePhones(List<PhoneDTO> phones) {
        if (phones.stream().filter(PhoneDTO::isPrincipal).count() != 1) {
            throw new BadRequestException("Debe haber exactamente un teléfono principal");
        }
        if (phones.stream().map(PhoneDTO::getNumber).distinct().count() != phones.size()) {
            throw new BadRequestException("Hay números de teléfono repetidos");
        }
    } // Cierre del método

    /**
     * Método que busca un teléfono dentro de la cuenta
     *
     * @param user La cuenta dueña del teléfono
     * @param phoneId El id del teléfono
     * @return PhoneModel El teléfono, para editarlo o eliminarlo, así nadie
     * puede modificar teléfonos de otra cuenta
     */
    private PhoneModel findPhone(UserModel user, UUID phoneId) {
        return user.getPhones().stream()
                .filter(phone -> phoneId.equals(phone.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Teléfono no encontrado: " + phoneId));
    } // Cierre del método

    /**
     * Método que verifica que un número no esté ya registrado en la cuenta
     *
     * @param user La cuenta a revisar
     * @param number El número que se quiere usar
     * @param excluded El teléfono que se está editando o nulo si es teléfono nuevo
     */
    private void checkNumberAvailable(UserModel user, String number, PhoneModel excluded) {
        if (user.getPhones().stream().anyMatch(phone -> phone != excluded && phone.getNumber().equals(number))) {
            throw new ConflictException("Ese número ya está registrado en la cuenta");
        }
    } // Cierre del método

    /**
     * Método que crea la excepción de usuario no encontrado
     *
     * @param id El id que no se encontró
     * @return ResourceNotFoundException La excepción 404 para lanzarla
     * con el mismo mensaje en todos los métodos
     */
    private ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException("Usuario no encontrado: " + id);
    } // Cierre del método
} // Cierre de la clase