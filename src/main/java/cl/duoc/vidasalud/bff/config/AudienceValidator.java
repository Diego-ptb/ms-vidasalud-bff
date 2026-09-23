package cl.duoc.vidasalud.bff.config;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza tokens emitidos para otra aplicación: comprueba que el claim "aud"
 * contenga alguna de las audiencias configuradas para esta API.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowedAudiences;

    public AudienceValidator(List<String> allowedAudiences) {
        this.allowedAudiences = allowedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (allowedAudiences.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }
        List<String> audiencias = token.getAudience();

        // Un token sin claim "aud" no puede validarse: se rechaza, nunca se
        // deja pasar por omision.
        boolean match = audiencias != null && audiencias.stream().anyMatch(allowedAudiences::contains);
        if (match) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "La audiencia del token no corresponde a esta API. aud=" + audiencias,
                null));
    }
}
