package cl.duoc.vidasalud.bff.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * La validacion de audiencia es uno de los puntos que evalua la pauta: el BFF
 * debe rechazar tokens emitidos para otra aplicacion, aunque vengan firmados
 * por el mismo tenant.
 */
class AudienceValidatorTest {

    private static final String API = "api://afd0d654-0000-0000-0000-000000000000";
    private static final String CLIENT_ID = "afd0d654-0000-0000-0000-000000000000";

    private Jwt tokenPara(String... audiencias) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "usuario")
                .audience(List.of(audiencias))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .claims(c -> c.put("roles", List.of("ADMIN")))
                .build();
    }

    @Test
    @DisplayName("Acepta el token cuya audiencia es la API")
    void aceptaLaAudienciaEsperada() {
        var validator = new AudienceValidator(List.of(API, CLIENT_ID));

        assertThat(validator.validate(tokenPara(API)).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Acepta el client id pelado, que es lo que traen los tokens v2")
    void aceptaElClientIdSinPrefijo() {
        // Entra ID pone en "aud" el client id sin el prefijo api:// cuando el
        // registro usa requestedAccessTokenVersion 2.
        var validator = new AudienceValidator(List.of(API, CLIENT_ID));

        assertThat(validator.validate(tokenPara(CLIENT_ID)).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Rechaza un token emitido para otra aplicacion")
    void rechazaOtraAudiencia() {
        var validator = new AudienceValidator(List.of(API));

        var resultado = validator.validate(tokenPara("api://otra-app"));

        assertThat(resultado.hasErrors()).isTrue();
        assertThat(resultado.getErrors().iterator().next().getDescription())
                .contains("audiencia");
    }

    @Test
    @DisplayName("Acepta si alguna de las audiencias del token calza")
    void bastaConQueUnaAudienciaCalce() {
        var validator = new AudienceValidator(List.of(API));

        assertThat(validator.validate(tokenPara("api://otra-app", API)).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Sin audiencias configuradas no bloquea")
    void sinConfiguracionNoBloquea() {
        // Evita dejar la API inaccesible por una configuracion incompleta; la
        // validacion de firma e issuer sigue vigente de todos modos.
        var validator = new AudienceValidator(List.of());

        assertThat(validator.validate(tokenPara("api://cualquiera")).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Un token sin audiencia es rechazado")
    void rechazaTokenSinAudiencia() {
        var validator = new AudienceValidator(List.of(API));

        Jwt sinAudiencia = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(Map.of("sub", "usuario")))
                .build();

        assertThat(validator.validate(sinAudiencia).hasErrors()).isTrue();
    }
}
