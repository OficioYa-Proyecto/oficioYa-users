package edu.eci.dosw.oficioya_users.controller;

import edu.eci.dosw.oficioya_users.dto.LocalityDTO;
import edu.eci.dosw.oficioya_users.security.SecurityConfig;
import edu.eci.dosw.oficioya_users.service.LocalityService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-14. El catálogo es público. Seguridad real. */
@WebMvcTest(LocalityController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=" + LocalityControllerTest.SECRET)
class LocalityControllerTest {

    static final String SECRET = "IadhiSp/AVyloXuE15l29oZzpucys4NEfqpHn6PEMeQ=";
    private static final String LOCALITIES = "/api/v1/localities";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private LocalityService localityService;

    private static String token() {
        return "Bearer " + Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("CONTRATANTE"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    }

    @Test
    void catalogIsPublic() throws Exception {
        when(localityService.findAll()).thenReturn(List.of(new LocalityDTO(1L, "Usaquén")));

        mockMvc.perform(get(LOCALITIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Usaquén"));
    }

    @Test
    void workersOfLocalityEndpointWasRemoved() throws Exception {
        mockMvc.perform(get(LOCALITIES + "/1/workers").header("Authorization", token()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(localityService);
    }
}