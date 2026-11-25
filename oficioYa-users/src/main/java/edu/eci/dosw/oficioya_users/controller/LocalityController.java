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

/**
 * Clase controladora que expone el catálogo de localidades de Bogotá (RF-14).
 * Es pública (no requiere token) y delega en LocalityService
 *
 * @author Daniel Felipe Sua
 * @author David Felipe Ortiz Salcedo
 * @author Juan Pablo Duarte Silva
 * @author Nicolás Esteban Delgado Rodríguez
 * @version 24/11/2026
 */
@RestController
@RequestMapping("/api/v1/localities")
@RequiredArgsConstructor
@Tag(name = "Localidades", description = "Catálogo de localidades de Bogotá (localidad del trabajador y del contratante)")
public class LocalityController {

    private final LocalityService localityService;

    /**
     * Método que atiende el endpoint GET /api/v1/localities
     *
     * @return List<LocalityDTO> Las localidades en orden alfabético
     * para llenar los selectores de registro y de perfil
     */
    @GetMapping
    @Operation(summary = "Consultar localidades", description = "RF-14. Catálogo de localidades. No requiere token")
    public List<LocalityDTO> findAll() {
        return localityService.findAll();
    } // Cierre del método
} // Cierre de la clase