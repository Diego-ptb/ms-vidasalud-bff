package cl.duoc.vidasalud.bff.web;

import java.util.LinkedHashMap;
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
        List<String> audiencias = jwt.getAudience();

        // Se usa LinkedHashMap y no Map.of porque este ultimo lanza
        // NullPointerException si algun claim viene ausente.
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("subject", String.valueOf(jwt.getSubject()));
        respuesta.put("name", String.valueOf(jwt.getClaimAsString("name")));
        respuesta.put("username", String.valueOf(jwt.getClaimAsString("preferred_username")));
        respuesta.put("roles", roles == null ? List.of() : roles);
        respuesta.put("issuer", String.valueOf(jwt.getIssuer()));
        respuesta.put("audience", audiencias == null ? List.of() : audiencias);
        respuesta.put("expiresAt", String.valueOf(jwt.getExpiresAt()));
        return respuesta;
    }
}
