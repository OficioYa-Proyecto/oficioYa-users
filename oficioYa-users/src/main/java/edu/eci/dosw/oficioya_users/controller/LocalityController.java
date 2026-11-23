package edu.eci.dosw.oficioya_users.controller;

import edu.eci.dosw.oficioya_users.dto.LocalityDTO;
import edu.eci.dosw.oficioya_users.service.LocalityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/localities")
@RequiredArgsConstructor
@Tag(name = "Localidades", description = "Catálogo de localidades de Bogotá (localidad del contratante)")
public class LocalityController {

    private final LocalityService localityService;

    @GetMapping
    @Operation(summary = "Consultar localidades", description = "RF-14. Catálogo de localidades. No requiere token")
    public List<LocalityDTO> findAll() {
        return localityService.findAll();
    }
}