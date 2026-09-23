package cl.duoc.vidasalud.bff.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import cl.duoc.vidasalud.bff.client.DownstreamClient;

/**
 * Comprueba los dos comportamientos que evalua la pauta del BFF: que rechace
 * peticiones sin token valido (401) y que aplique autorizacion por rol (403).
 *
 * El JwtDecoder se sustituye por un mock para no depender de la red: las
 * pruebas no deben necesitar acceso al tenant real de Entra ID.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/test-tenant/v2.0",
        "vidasalud.security.allowed-audiences=api://test-api"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private DownstreamClient downstreamClient;

    /**
     * El proxy hacia los microservicios se simula: estas pruebas verifican la
     * capa de seguridad, no la integracion con el dominio.
     */
    @BeforeEach
    void simularDownstream() {
        when(downstreamClient.exchange(any(), anyString(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok("[]"));
    }

    /** Token valido con los roles indicados, ya validado por el resource server. */
    private RequestPostProcessor comoUsuarioCon(String... roles) {
        return jwt()
                .jwt(builder -> builder
                        .claim("roles", List.of(roles))
                        .claim("name", "Usuario de Prueba")
                        .claim("preferred_username", "usuario@vidasalud.cl"))
                .authorities(List.of(roles).stream()
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                        .toList());
    }

    // ---------------------------------------------------------------- 401

    @Test
    @DisplayName("Sin token responde 401 con el JSON de error propio")
    void sinTokenRespondeUnauthorized() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Los endpoints de dominio tambien exigen token")
    void todosLosEndpointsExigenToken() throws Exception {
        mvc.perform(get("/api/appointments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/catalog/services")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/report/kpis")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/audit/timeline")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El health check queda publico, para que el balanceador lo consulte")
    void elHealthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- 403

    @Test
    @DisplayName("Un paciente no puede ver la reporteria")
    void elPacienteNoVeReporteria() throws Exception {
        mvc.perform(get("/api/report/kpis").with(comoUsuarioCon("PACIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @DisplayName("Un paciente no puede ver la auditoria")
    void elPacienteNoVeAuditoria() throws Exception {
        mvc.perform(get("/api/audit/timeline").with(comoUsuarioCon("PACIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un paciente no puede cambiar el estado de una atencion")
    void elPacienteNoCambiaEstados() throws Exception {
        mvc.perform(put("/api/appointments/1/status")
                        .with(comoUsuarioCon("PACIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMADA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un recepcionista no puede escribir en el catalogo")
    void elRecepcionistaNoEditaElCatalogo() throws Exception {
        mvc.perform(post("/api/catalog/services")
                        .with(comoUsuarioCon("RECEPCIONISTA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un usuario sin roles no accede a nada")
    void sinRolesNoAccedeANada() throws Exception {
        mvc.perform(get("/api/appointments").with(comoUsuarioCon()))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- 200

    @Test
    @DisplayName("/api/me devuelve los claims que el BFF leyo del token")
    void meDevuelveLosClaimsDelToken() throws Exception {
        mvc.perform(get("/api/me").with(comoUsuarioCon("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Usuario de Prueba"))
                .andExpect(jsonPath("$.username").value("usuario@vidasalud.cl"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("El admin si entra a la reporteria")
    void elAdminVeReporteria() throws Exception {
        mvc.perform(get("/api/report/kpis").with(comoUsuarioCon("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El auditor entra a la auditoria pero no a la reporteria")
    void elAuditorSoloVeAuditoria() throws Exception {
        mvc.perform(get("/api/audit/timeline").with(comoUsuarioCon("AUDITOR")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/report/kpis").with(comoUsuarioCon("AUDITOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El paciente si puede leer el catalogo, porque necesita elegir prestacion")
    void elPacienteLeeElCatalogo() throws Exception {
        mvc.perform(get("/api/catalog/services").with(comoUsuarioCon("PACIENTE")))
                .andExpect(status().isOk());
    }
}
