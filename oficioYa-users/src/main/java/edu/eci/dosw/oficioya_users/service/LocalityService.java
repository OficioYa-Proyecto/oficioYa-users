package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.dto.LocalityDTO;
import edu.eci.dosw.oficioya_users.repository.LocalityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Clase de servicio del catálogo de localidades (RF-14)
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz
 * @author Juan Pablo Duarte
 * @author Nicolas Esteban Delgado
 * @version 24/11/2026
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalityService {

    private final LocalityRepository localityRepository;

    /**
     * Método que obtiene el catálogo dde localidades
     *
     * @return List<LocalityDTO> Las localidades en orden alfabético [para front-end]
     */
    public List<LocalityDTO> findAll() {
        return localityRepository.findAllByOrderByNameAsc().stream()
                .map(locality -> new LocalityDTO(locality.getId(), locality.getName()))
                .toList();
    } // Cierre del método
} // Cierre de la clase