package edu.eci.dosw.oficioya_users.repository;

import edu.eci.dosw.oficioya_users.entity.PhoneEntity;
import edu.eci.dosw.oficioya_users.entity.RoleEntity;
import edu.eci.dosw.oficioya_users.entity.UserEntity;
import edu.eci.dosw.oficioya_users.enums.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Clase de pruebas de UserRepository con una base de datos en memoria (@DataJpaTest)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TestEntityManager entityManager;

    /**
     * Método que guarda una cuenta de prueba con dos teléfonos (uno principal) y un rol de prueba.
     *
     * @param email El correo.
     * @param status El estado de la cuenta.
     * @return UserEntity La cuenta guardada.
     */
    private UserEntity newUser(String email, AccountStatus status) {
        RoleEntity roleRepo = roleRepository.findByName("PRUEBA").orElseGet(() -> {
            RoleEntity role = new RoleEntity();
            role.setName("PRUEBA");
            return roleRepository.save(role);
        });
        PhoneEntity principal = new PhoneEntity();
        principal.setNumber("3001234567");
        principal.setPrincipal(true);
        PhoneEntity secondary = new PhoneEntity();
        secondary.setNumber("3109876543");

        UserEntity user = new UserEntity();
        user.setName("Ana Pérez");
        user.setEmail(email);
        user.setAccountStatus(status);
        user.setPhones(List.of(principal, secondary));
        user.setRoles(new HashSet<>(Set.of(roleRepo)));
        UserEntity saved = userRepository.saveAndFlush(user);
        entityManager.clear();
        return saved;
    } // Cierre del método

    /**
     * Método que guarda una cuenta con fechas de creación y desactivación dadas, para las pruebas de RF-12.
     *
     * @param email El correo.
     * @param createdAt La fecha de creación.
     * @param deactivatedAt La fecha de desactivación, o null.
     * @param roleNames Los roles de la cuenta.
     */
    private void account(String email, LocalDateTime createdAt, LocalDateTime deactivatedAt, String... roleNames) {
        Set<RoleEntity> roles = new HashSet<>();
        for (String name : roleNames) {
            roles.add(roleRepository.findByName(name).orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName(name);
                return roleRepository.save(role);
            }));
        }
        UserEntity user = new UserEntity();
        user.setName("Cuenta");
        user.setEmail(email);
        user.setAccountStatus(deactivatedAt == null ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
        user.setDeactivatedAt(deactivatedAt);
        user.setRoles(roles);
        UUID id = userRepository.saveAndFlush(user).getId();
        // created_at no se puede cambiar desde la entidad
        entityManager.getEntityManager().createNativeQuery("UPDATE users SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt).setParameter("id", id).executeUpdate();
        entityManager.clear();
    } // Cierre del método

    /**
     * Prueba que se guarda una cuenta con teléfonos, roles y fechas.
     */
    @Test
    void savesUserWithPhonesRolesAndDates() {
        UserEntity saved = newUser("ana@mail.com", null);

        UserEntity found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getPhones()).hasSize(2);
        assertThat(found.getRoles()).extracting(RoleEntity::getName).containsExactly("PRUEBA");
        assertThat(userRepository.existsByEmail("ana@mail.com")).isTrue();
        assertThat(userRepository.existsByEmail("otro@mail.com")).isFalse();
    } // Cierre del método

    /**
     * Prueba que la búsqueda por id y estado ignora las cuentas inactivas.
     */
    @Test
    void findByIdAndAccountStatusIgnoresInactiveUsers() {
        UserEntity active = newUser("activo@mail.com", AccountStatus.ACTIVE);
        UserEntity inactive = newUser("inactivo@mail.com", AccountStatus.INACTIVE);

        assertThat(userRepository.findByIdAndAccountStatus(active.getId(), AccountStatus.ACTIVE)).isPresent();
        assertThat(userRepository.findByIdAndAccountStatus(inactive.getId(), AccountStatus.ACTIVE)).isEmpty();
    } // Cierre del método

    /**
     * Prueba que la búsqueda de varios ids devuelve solo las cuentas activas.
     */
    @Test
    void findByIdInAndAccountStatusReturnsOnlyActiveUsers() {
        UserEntity active = newUser("activo@mail.com", AccountStatus.ACTIVE);
        UserEntity inactive = newUser("inactivo@mail.com", AccountStatus.INACTIVE);

        List<UserEntity> found = userRepository.findByIdInAndAccountStatus(
                List.of(active.getId(), inactive.getId(), UUID.randomUUID()), AccountStatus.ACTIVE);

        assertThat(found).extracting(UserEntity::getId).containsExactly(active.getId());
    } // Cierre del método

    /**
     * Prueba que al reemplazar los teléfonos se borran los anteriores.
     */
    @Test
    void replacingPhonesRemovesOldOnes() {
        UserEntity saved = newUser("ana@mail.com", AccountStatus.ACTIVE);
        UserEntity found = userRepository.findById(saved.getId()).orElseThrow();
        PhoneEntity phone = new PhoneEntity();
        phone.setNumber("3205554444");
        phone.setPrincipal(true);

        found.setPhones(List.of(phone));
        userRepository.saveAndFlush(found);

        Long phones = entityManager.getEntityManager()
                .createQuery("select count(p) from PhoneEntity p", Long.class).getSingleResult();
        assertThat(phones).isEqualTo(1L);
        assertThat(found.getUpdatedAt()).isNotNull();
    } // Cierre del método

    /**
     * Prueba que el correo es único en la base de datos.
     */
    @Test
    void emailMustBeUnique() {
        newUser("ana@mail.com", AccountStatus.ACTIVE);

        assertThrows(DataIntegrityViolationException.class, () -> newUser("ana@mail.com", AccountStatus.ACTIVE));
    } // Cierre del método

    /**
     * Prueba que la búsqueda por correo devuelve la cuenta sin importar su estado.
     */
    @Test
    void findByEmailReturnsAccountRegardlessOfStatus() {
        UserEntity inactive = newUser("inactivo@mail.com", AccountStatus.INACTIVE);

        assertThat(userRepository.findByEmail("inactivo@mail.com")).get()
                .extracting(UserEntity::getId).isEqualTo(inactive.getId());
        assertThat(userRepository.findByEmail("otro@mail.com")).isEmpty();
    } // Cierre del método

    /**
     * Prueba que se guarda la fecha de desactivación.
     */
    @Test
    void savesDeactivationDate() {
        UserEntity inactive = newUser("inactivo@mail.com", AccountStatus.INACTIVE);
        LocalDateTime date = LocalDateTime.of(2026, 11, 1, 10, 30);
        inactive.setDeactivatedAt(date);
        userRepository.saveAndFlush(inactive);
        entityManager.clear();

        assertThat(userRepository.findById(inactive.getId()).orElseThrow().getDeactivatedAt()).isEqualTo(date);
    } // Cierre del método

    /**
     * Prueba que las consultas de RF-12 cuentan bien las registradas hasta la fecha de fin y las desactivadas
     * en el periodo.
     */
    @Test
    void countsAccountsByRoleRegisteredUntilEndAndDeactivatedInPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 0, 0);
        account("trabajador@mail.com", start.minusDays(10), null, "TRABAJADOR");
        account("ambos@mail.com", start.plusDays(3), start.plusDays(5), "TRABAJADOR", "CONTRATANTE");
        account("antes@mail.com", start.minusDays(40), start.minusDays(2), "CONTRATANTE");
        account("despues@mail.com", end, null, "CONTRATANTE");
        account("admin@mail.com", start, null, "ADMIN");
        List<String> both = List.of("TRABAJADOR", "CONTRATANTE");

        assertThat(userRepository.countRegistered(both, end)).isEqualTo(3);
        assertThat(userRepository.countRegistered(List.of("TRABAJADOR"), end)).isEqualTo(2);
        assertThat(userRepository.countRegistered(List.of("CONTRATANTE"), end)).isEqualTo(2);
        assertThat(userRepository.countDeactivated(both, start, end)).isEqualTo(1);
        assertThat(userRepository.countDeactivated(List.of("TRABAJADOR"), start, end)).isEqualTo(1);
        assertThat(userRepository.countDeactivated(List.of("CONTRATANTE"), start, end)).isEqualTo(1);
    } // Cierre del método

    /**
     * Prueba que la sentencia de data.sql completa la fecha de desactivación de las cuentas inactivas que no
     * la tienen.
     */
    @Test
    void dataSqlFillsDeactivationDateOfOldInactiveAccounts() throws IOException {
        UserEntity active = newUser("activo@mail.com", AccountStatus.ACTIVE);
        UserEntity oldInactive = newUser("inactivo@mail.com", AccountStatus.INACTIVE);
        String backfill = new ClassPathResource("data.sql").getContentAsString(StandardCharsets.UTF_8).lines()
                .filter(line -> line.startsWith("UPDATE users SET deactivated_at"))
                .findFirst().orElseThrow();

        entityManager.getEntityManager().createNativeQuery(backfill).executeUpdate();
        entityManager.clear();

        UserEntity filled = userRepository.findById(oldInactive.getId()).orElseThrow();
        assertThat(filled.getDeactivatedAt()).isEqualTo(filled.getUpdatedAt());
        assertThat(userRepository.findById(active.getId()).orElseThrow().getDeactivatedAt()).isNull();
    } // Cierre del método
} // Cierre de la clase