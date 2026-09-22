package cl.duoc.vidasalud.bff.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de verificacion: devuelve lo que el BFF leyo del token ya validado.
 * Sirve para demostrar en la evaluacion que la validacion funciona.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return Map.of(
                "subject", String.valueOf(jwt.getSubject()),
                "name", String.valueOf(jwt.getClaimAsString("name")),
                "username", String.valueOf(jwt.getClaimAsString("preferred_username")),
                "roles", roles == null ? List.of() : roles,
                "issuer", String.valueOf(jwt.getIssuer()),
                "audience", jwt.getAudience(),
                "expiresAt", String.valueOf(jwt.getExpiresAt()));
    }
}
