package edu.eci.dosw.oficioya_users.service;

import edu.eci.dosw.oficioya_users.dto.LocalityDTO;
import edu.eci.dosw.oficioya_users.repository.LocalityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalityService {

    private final LocalityRepository localityRepository;

    /** RF-14. Catálogo de localidades, ordenado alfabéticamente */
    public List<LocalityDTO> findAll() {
        return localityRepository.findAllByOrderByNameAsc().stream()
                .map(locality -> new LocalityDTO(locality.getId(), locality.getName()))
                .toList();
    }
}