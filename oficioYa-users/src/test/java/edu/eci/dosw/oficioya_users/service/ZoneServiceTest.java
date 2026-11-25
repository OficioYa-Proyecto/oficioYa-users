package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.dto.ZoneDTO;
import edu.eci.dosw.oficioya_users.entity.ZoneEntity;
import edu.eci.dosw.oficioya_users.exception.BadRequestException;
import edu.eci.dosw.oficioya_users.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas de ZoneService con el repositorio simulado (Mockito)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;
    @InjectMocks
    private ZoneService zoneService;

    /**
     * Método que crea una zona.
     *
     * @param id El id.
     * @param name El nombre del barrio.
     * @param locality La localidad.
     * @return ZoneEntity La zona.
     */
    private static ZoneEntity zone(long id, String name, String locality) {
        ZoneEntity zone = new ZoneEntity();
        zone.setId(id);
        zone.setName(name);
        zone.setLocality(locality);
        return zone;
    } // Cierre del método

    /**
     * Prueba que se devuelve todo el catálogo.
     */
    @Test
    void findAllReturnsWholeCatalog() {
        when(zoneRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(zone(1, "Cedritos", "Usaquén"), zone(2, "Santa Bárbara", "Usaquén")));

        assertThat(zoneService.findAll()).containsExactly(
                new ZoneDTO(1L, "Cedritos", "Usaquén"),
                new ZoneDTO(2L, "Santa Bárbara", "Usaquén"));
    } // Cierre del método

    /**
     * Prueba que se quitan los ids repetidos y se devuelven solo las zonas que existen.
     */
    @Test
    void findByIdsRemovesDuplicatesAndReturnsOnlyExisting() {
        when(zoneRepository.findByIdInOrderByIdAsc(Set.of(13L, 2L, 999L)))
                .thenReturn(List.of(zone(2, "Santa Bárbara", "Usaquén"), zone(13, "Teusaquillo", "Teusaquillo")));

        List<ZoneDTO> zones = zoneService.findByIds(List.of(13L, 2L, 2L, 999L));

        assertThat(zones).extracting(ZoneDTO::id).containsExactly(2L, 13L);
        verify(zoneRepository).findByIdInOrderByIdAsc(Set.of(13L, 2L, 999L));
    } // Cierre del método

    /**
     * Prueba que se aceptan exactamente 50 ids.
     */
    @Test
    void findByIdsAcceptsExactlyTheLimit() {
        List<Long> ids = LongStream.rangeClosed(1, ZoneService.MAX_IDS).boxed().toList();
        when(zoneRepository.findByIdInOrderByIdAsc(Set.copyOf(ids))).thenReturn(List.of());

        assertThat(zoneService.findByIds(ids)).isEmpty();
    } // Cierre del método

    /**
     * Prueba que se rechazan más de 50 ids (400).
     */
    @Test
    void findByIdsRejectsMoreThanTheLimit() {
        List<Long> ids = LongStream.rangeClosed(1, ZoneService.MAX_IDS + 1L).boxed().toList();

        BadRequestException exception = assertThrows(BadRequestException.class, () -> zoneService.findByIds(ids));
        assertThat(exception.getMessage()).contains("50");
        verifyNoInteractions(zoneRepository);
    } // Cierre del método
} // Cierre de la clase
