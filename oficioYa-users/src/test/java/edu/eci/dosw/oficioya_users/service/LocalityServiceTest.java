package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.dto.LocalityDTO;
import edu.eci.dosw.oficioya_users.entity.LocalityEntity;
import edu.eci.dosw.oficioya_users.repository.LocalityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas de LocalityService con el repositorio simulado (Mockito)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@ExtendWith(MockitoExtension.class)
class LocalityServiceTest {

    @Mock
    private LocalityRepository localityRepository;
    @InjectMocks
    private LocalityService localityService;

    /**
     * Prueba que se devuelve el catálogo de localidades.
     */
    @Test
    void findAllReturnsCatalog() {
        LocalityEntity suba = new LocalityEntity();
        suba.setId(11L);
        suba.setName("Suba");
        when(localityRepository.findAllByOrderByNameAsc()).thenReturn(List.of(suba));

        assertThat(localityService.findAll()).containsExactly(new LocalityDTO(11L, "Suba"));
    } // Cierre del método
} // Cierre de la clase